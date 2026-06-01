package com.example.orderservice.e2e;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.*;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Uçtan uca alışveriş akışı testi.
 *
 * Ön koşul: tüm servisler ve auth-server çalışıyor olmalı.
 * Çalıştırma: mvn test -pl order-service -Dgroups=e2e
 *
 * Akış:
 *   1. admin/admin123 → OAuth2 authorization_code flow → JWT token
 *   2. Ürün kataloğundan 500 TRY altı ürün bul (Wireless Mouse vb.)
 *   3. Ürünü sepete ekle
 *   4. Sepeti görüntüle — ürün var mı doğrula
 *   5. Sepetten değil, doğrudan sipariş oluştur
 *   6. Ödeme başlat (< 500 TRY → FRICTIONLESS, 3DS challenge YOK)
 *   7. Sipariş durumu → CONFIRMED doğrula
 */
@Tag("e2e")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("🛒 Admin Alışveriş Akışı — E2E")
class ShoppingFlowE2ETest {

    /* ── Servis adresleri ───────────────────────────────────────────────────── */
    private static final String AUTH    = "http://localhost:9000";
    private static final String GATEWAY = "http://localhost:8090";

    /* ── Paylaşılan test durumu (static — sıralı testler arası taşınır) ─────── */
    private static String TOKEN;
    private static Long   PRODUCT_ID;
    private static double PRODUCT_PRICE;
    private static Long   ORDER_ID;

    private static final ObjectMapper JSON = new ObjectMapper();

    @BeforeAll
    static void requireExternalServices() {
        assumeTrue(isServiceReachable(AUTH), "⚠️  auth-server çalışmıyor; E2E testler skip ediliyor");
        assumeTrue(isServiceReachable(GATEWAY), "⚠️  api-gateway çalışmıyor; E2E testler skip ediliyor");
    }

    /* ══════════════════════════════════════════════════════════════════════════
       ADIM 1 — TOKEN
    ══════════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(1)
    @DisplayName("1 ▸ admin/admin123 ile OAuth2 token al")
    void step1_token_al() throws Exception {
        TOKEN = obtainToken("admin", "admin123");

        assertThat(TOKEN)
            .as("Token alınamadı — auth-server (localhost:9000) çalışıyor mu?")
            .isNotBlank()
            .contains(".");

        System.out.println("✅ Token alındı: " + TOKEN.substring(0, 30) + "…");
    }

    /* ══════════════════════════════════════════════════════════════════════════
       ADIM 2 — ÜRÜN BUL (< 500 TRY)
    ══════════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(2)
    @DisplayName("2 ▸ Katalogdan 500 TRY altı ürün bul")
    void step2_urun_bul() throws Exception {
        assumeTrue(TOKEN != null, "⚠️  Adım 1 tamamlanmadı, token yok");

        // GET /api/products — public endpoint, auth gerekmez
        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> r = http.send(
            HttpRequest.newBuilder()
                .GET().uri(URI.create(GATEWAY + "/api/products"))
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString()
        );
        assertThat(r.statusCode()).as("Ürün listesi alınamadı").isEqualTo(200);

        JsonNode products = JSON.readTree(r.body());
        assertThat(products.isArray()).isTrue();

        for (JsonNode p : products) {
            double price = p.path("price").asDouble();
            long   id    = p.path("id").asLong();
            String name  = p.path("name").asText();
            int    stock = p.path("stock").asInt();

            if (price > 0 && price < 500 && stock > 0) {
                PRODUCT_ID    = id;
                PRODUCT_PRICE = price;
                System.out.printf("✅ Seçilen ürün: id=%d name='%s' price=%.2f TRY stock=%d%n",
                    id, name, price, stock);
                break;
            }
        }

        assertThat(PRODUCT_ID)
            .as("Katalogda 500 TRY altı, stoklu ürün bulunamadı")
            .isNotNull()
            .isPositive();
    }

    /* ══════════════════════════════════════════════════════════════════════════
       ADIM 3 — SEPETE EKLE
    ══════════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(3)
    @DisplayName("3 ▸ Seçilen ürünü sepete ekle")
    void step3_sepete_ekle() throws Exception {
        assumeTrue(TOKEN      != null, "⚠️  Token yok");
        assumeTrue(PRODUCT_ID != null, "⚠️  Ürün seçilmedi");

        String body = """
            {"productId": %d, "quantity": 1}
            """.formatted(PRODUCT_ID);

        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> r = http.send(
            postJson(GATEWAY + "/api/cart/items", body),
            BodyHandlers.ofString()
        );

        System.out.println("Sepet ekle yanıtı: " + r.statusCode() + " | " + r.body());
        assertThat(r.statusCode())
            .as("Sepete ekleme başarısız. Body: " + r.body())
            .isIn(200, 201);
    }

    /* ══════════════════════════════════════════════════════════════════════════
       ADIM 4 — SEPETİ GÖRÜNTÜLE
    ══════════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(4)
    @DisplayName("4 ▸ Sepeti görüntüle — ürün mevcut mu kontrol et")
    void step4_sepet_goruntule() throws Exception {
        assumeTrue(TOKEN != null, "⚠️  Token yok");

        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> r = http.send(
            getAuth(GATEWAY + "/api/cart"),
            BodyHandlers.ofString()
        );

        System.out.println("Sepet: " + r.statusCode() + " | " + r.body());
        assertThat(r.statusCode()).as("Sepet alınamadı").isEqualTo(200);

        JsonNode cart = JSON.readTree(r.body());
        // Sepet response'u "items" array'i veya direkt array olabilir
        JsonNode items = cart.isArray() ? cart : cart.path("items");
        assertThat(items.isArray() && items.size() > 0)
            .as("Sepet boş — ürün eklenmedi olabilir")
            .isTrue();

        System.out.println("✅ Sepette " + items.size() + " kalem var");
    }

    /* ══════════════════════════════════════════════════════════════════════════
       ADIM 5 — SİPARİŞ OLUŞTUR
    ══════════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(5)
    @DisplayName("5 ▸ Sipariş oluştur (productId=" + "PRODUCT_ID" + ", qty=1)")
    void step5_siparis_olustur() throws Exception {
        assumeTrue(TOKEN      != null, "⚠️  Token yok");
        assumeTrue(PRODUCT_ID != null, "⚠️  Ürün seçilmedi");

        String body = """
            {"productId": %d, "quantity": 1}
            """.formatted(PRODUCT_ID);

        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> r = http.send(
            postJson(GATEWAY + "/api/orders", body),
            BodyHandlers.ofString()
        );

        System.out.println("Sipariş yanıtı: " + r.statusCode() + " | " + r.body());
        assertThat(r.statusCode())
            .as("Sipariş oluşturulamadı. Body: " + r.body())
            .isIn(200, 201);

        JsonNode order = JSON.readTree(r.body());
        ORDER_ID = order.path("id").asLong();

        assertThat(ORDER_ID).as("orderId eksik").isPositive();
        assertThat(order.path("status").asText())
            .as("Yeni sipariş PENDING olmalı")
            .isEqualTo("PENDING");

        System.out.printf("✅ Sipariş oluşturuldu: orderId=%d status=%s totalPrice=%.2f%n",
            ORDER_ID,
            order.path("status").asText(),
            order.path("totalPrice").asDouble());
    }

    /* ══════════════════════════════════════════════════════════════════════════
       ADIM 6 — ÖDEME YAP (FRICTIONLESS — < 500 TRY)
    ══════════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(6)
    @DisplayName("6 ▸ Ödeme başlat — FRICTIONLESS (3DS challenge BEKLENMİYOR)")
    void step6_odeme_yap() throws Exception {
        assumeTrue(TOKEN    != null, "⚠️  Token yok");
        assumeTrue(ORDER_ID != null, "⚠️  Sipariş oluşturulmadı");

        assertThat(PRODUCT_PRICE)
            .as("Ürün fiyatı 500 TRY'den küçük olmalı — FRICTIONLESS için")
            .isLessThan(500.0);

        System.out.printf("ℹ️  Ödeme: orderId=%d amount=%.2f TRY → FRICTIONLESS bekleniyor%n",
            ORDER_ID, PRODUCT_PRICE);

        String body = """
            {
              "orderId": %d,
              "amount": %.2f,
              "paymentMethod": "CREDIT_CARD"
            }
            """.formatted(ORDER_ID, PRODUCT_PRICE);

        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> r = http.send(
            postJson(GATEWAY + "/api/payments/initiate", body),
            BodyHandlers.ofString()
        );

        System.out.println("Payment response: " + r.statusCode() + " | " + r.body());
        assertThat(r.statusCode())
            .as("Payment endpoint returned an error. Body: " + r.body())
            .isEqualTo(200);

        JsonNode payment = JSON.readTree(r.body());

        // PaymentInitiateResponse record alanları: type, status (threeDsType/threeDsStatus DEĞİL)
        assertThat(payment.path("type").asText())
            .as("Payments below 500 TRY should be FRICTIONLESS")
            .isEqualTo("FRICTIONLESS");

        assertThat(payment.path("status").asText())
            .as("FRICTIONLESS payment should be SUCCESS")
            .isEqualTo("SUCCESS");

        System.out.printf("✅ Payment successful: type=%s status=%s transactionId=%s%n",
            payment.path("type").asText(),
            payment.path("status").asText(),
            payment.path("transactionId").asText());
    }

    /* ══════════════════════════════════════════════════════════════════════════
        STEP 7 — VERIFY ORDER STATUS
    ══════════════════════════════════════════════════════════════════════════ */

    @Test
    @Order(7)
    @DisplayName("7 ▸ Order status should be CONFIRMED after payment")
    void step7_siparis_durumu() throws Exception {
        assumeTrue(TOKEN    != null, "⚠️  Token missing");
        assumeTrue(ORDER_ID != null, "⚠️  Order missing");

        // Payment confirmation is asynchronous (callback to order-service) — short wait
        Thread.sleep(800);

        HttpClient http = HttpClient.newHttpClient();
        HttpResponse<String> r = http.send(
            getAuth(GATEWAY + "/api/orders/" + ORDER_ID),
            BodyHandlers.ofString()
        );

        System.out.println("Order status response: " + r.statusCode() + " | " + r.body());
        assertThat(r.statusCode()).as("Order could not be retrieved").isEqualTo(200);

        JsonNode order = JSON.readTree(r.body());
        String status = order.path("status").asText();

        assertThat(status)
            .as("After payment, the order should be CONFIRMED (orderId=" + ORDER_ID + ")")
            .isEqualTo("CONFIRMED");

        System.out.printf("✅ Order confirmed: orderId=%d status=%s%n", ORDER_ID, status);
        System.out.println("\n🎉 All steps completed successfully!");
    }

    /* ══════════════════════════════════════════════════════════════════════════
       YARDIMCI METODLAR
    ══════════════════════════════════════════════════════════════════════════ */

    /** GET request with Authorization header */
    private HttpRequest getAuth(String url) {
        return HttpRequest.newBuilder()
            .GET()
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + TOKEN)
            .header("Accept", "application/json")
            .build();
    }

    /** JSON POST request with Authorization header */
    private HttpRequest postJson(String url, String body) {
        return HttpRequest.newBuilder()
            .POST(BodyPublishers.ofString(body.strip()))
            .uri(URI.create(url))
            .header("Authorization", "Bearer " + TOKEN)
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .build();
    }

    /**
     * OAuth2 Authorization Code flow — programmatic.
     *
     * Steps:
     *   1. GET /oauth2/authorize        → 302 to /login  (stores pending auth in the session)
     *   2. GET /login                   → parse the CSRF token
     *   3. POST /login (user+pass+csrf) → logs in, 302 to /oauth2/authorize
     *   4. Follow the redirect chain    → redirect_uri?code=XXX
     *   5. POST /oauth2/token (code)    → obtain access_token
     */
    static String obtainToken(String username, String password) throws Exception {
        final String REDIRECT_URI = "http://127.0.0.1:8081/authorized";
        final String SCOPES = "openid catalog:read catalog:write "
            + "orders:read orders:write orders:manage cart:read cart:write "
            + "payments:read payments:write payments:manage "
            + "notifications:read notifications:write users:read users:manage";

        CookieManager cm = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient http = HttpClient.newBuilder()
            .cookieHandler(cm)
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();

        // ── 1. /oauth2/authorize → writes pending auth to the session, redirects to /login ──
        String authorizeUrl = AUTH + "/oauth2/authorize"
            + "?response_type=code"
            + "&client_id=client-app"
            + "&scope="        + URLEncoder.encode(SCOPES,        StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI,  StandardCharsets.UTF_8)
            + "&state=e2e-state";

        http.send(HttpRequest.newBuilder().GET().uri(URI.create(authorizeUrl)).build(),
            BodyHandlers.discarding());

        // ── 2. GET /login → fetch CSRF token ────────────────────────────────────────
        HttpResponse<String> loginPageResp = http.send(
            HttpRequest.newBuilder().GET().uri(URI.create(AUTH + "/login")).build(),
            BodyHandlers.ofString()
        );
        String csrf = extractCsrf(loginPageResp.body());
        System.out.println("ℹ️  CSRF token alındı: " + csrf.substring(0, 8) + "…");

        // ── 3. POST /login → logs in, redirects to /oauth2/authorize ────────────────
        String loginBody = "username=" + URLEncoder.encode(username, StandardCharsets.UTF_8)
            + "&password="      + URLEncoder.encode(password, StandardCharsets.UTF_8)
            + "&_csrf="         + URLEncoder.encode(csrf,     StandardCharsets.UTF_8);

        HttpResponse<String> loginResp = http.send(
            HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(loginBody))
                .uri(URI.create(AUTH + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .build(),
            BodyHandlers.ofString()
        );

        assertThat(loginResp.statusCode())
            .as("Login failed! Status: " + loginResp.statusCode()
                + "\nBody: " + loginResp.body().substring(0, Math.min(400, loginResp.body().length())))
            .isIn(301, 302);

        // ── 4. Redirect chain: /oauth2/authorize → redirect_uri?code=XXX ───────────
        String location = loginResp.headers().firstValue("Location")
            .orElseThrow(() -> new AssertionError("Login sonrası Location header yok!\nBody: "
                + loginResp.body().substring(0, Math.min(300, loginResp.body().length()))));

        System.out.println("ℹ️  Login sonrası yönlendirme: " + location);

        int hops = 6;
        while (!location.contains("127.0.0.1:8081") && !location.contains("localhost:8081") && hops-- > 0) {
            if (!location.startsWith("http")) location = AUTH + location;

            HttpResponse<String> r = http.send(
                HttpRequest.newBuilder().GET().uri(URI.create(location)).build(),
                BodyHandlers.ofString()
            );

            if (r.statusCode() == 200) {
                // Consent page — normally this should not happen because
                // "requireAuthorizationConsent=false" is set; if it does, submit the form automatically
                System.out.println("⚠️  Consent page encountered (unexpected)");
                throw new AssertionError("Auth server requested consent — DataInitializer should have "
                    + "set requireAuthorizationConsent(false)!");
            }

            location = r.headers().firstValue("Location")
                .orElseThrow(() -> new AssertionError(
                    "Redirect zinciri kırıldı! Status: " + r.statusCode()
                    + " Body: " + r.body().substring(0, Math.min(300, r.body().length()))));

            System.out.println("ℹ️  Hop → " + location);
        }

        assertThat(location)
            .as("redirect_uri (127.0.0.1:8081) was not reached")
            .contains("127.0.0.1:8081");

        // ── 5. extract the code value from the URL ───────────────────────────────────
        String queryString = URI.create(location).getQuery();
        assertThat(queryString).as("Redirect URL has no query string: " + location).isNotNull();

        String code = Arrays.stream(queryString.split("&"))
            .filter(p -> p.startsWith("code="))
            .map(p -> p.substring("code=".length()))
            .findFirst()
            .orElseThrow(() -> new AssertionError(
                "Auth code not found. Query: " + queryString));

        System.out.println("ℹ️  Auth code alındı: " + code.substring(0, 8) + "…");

        // ── 6. exchange the code for an access_token ────────────────────────────────
        String tokenBody = "grant_type=authorization_code"
            + "&code="         + URLEncoder.encode(code,          StandardCharsets.UTF_8)
            + "&redirect_uri=" + URLEncoder.encode(REDIRECT_URI,  StandardCharsets.UTF_8)
            + "&client_id=client-app"
            + "&client_secret=secret";

        HttpResponse<String> tokenResp = http.send(
            HttpRequest.newBuilder()
                .POST(BodyPublishers.ofString(tokenBody))
                .uri(URI.create(AUTH + "/oauth2/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .build(),
            BodyHandlers.ofString()
        );

        assertThat(tokenResp.statusCode())
            .as("Token endpoint hatası! Body: " + tokenResp.body())
            .isEqualTo(200);

        String accessToken = JSON.readTree(tokenResp.body()).path("access_token").asText();
        assertThat(accessToken).as("access_token boş").isNotBlank();

        return accessToken;
    }

    private static boolean isServiceReachable(String baseUrl) {
        try {
            HttpClient http = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();

            http.send(HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .timeout(Duration.ofSeconds(2))
                    .GET()
                    .build(),
                BodyHandlers.discarding());
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    /** HTML login formundan CSRF token'ı çıkarır */
    private static String extractCsrf(String html) {
        for (Pattern p : List.of(
            Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("value=\"([^\"]+)\"[^>]*name=\"_csrf\"", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\"_csrf\",\"([^\"]+)\"")
        )) {
            Matcher m = p.matcher(html);
            if (m.find()) return m.group(1);
        }
        throw new AssertionError(
            "CSRF token HTML'de bulunamadı!\nHTML (ilk 600 karakter):\n"
            + html.substring(0, Math.min(600, html.length()))
        );
    }
}

