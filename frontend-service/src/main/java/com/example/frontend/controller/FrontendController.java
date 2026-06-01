package com.example.frontend.controller;

import com.example.frontend.dto.*;
import com.example.frontend.model.*;
import com.example.frontend.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class FrontendController {

    private static final String DATA = "data";
    private static final Logger log = LoggerFactory.getLogger(FrontendController.class);

    private final ProductService      productService;
    private final CartService         cartService;
    private final GuestCartService    guestCartService;
    private final OrderService        orderService;
    private final PaymentService      paymentService;
    private final NotificationService notificationService;
    private final UserProfileService  userProfileService;

    // Public endpoint calls (registration, etc.) — includes trace propagation
    private final RestTemplate publicRestTemplate;

    @Value("${gateway.url:http://localhost:8090}")
    private String gatewayUrl;

    public FrontendController(ProductService productService,
                              CartService cartService,
                              GuestCartService guestCartService,
                              OrderService orderService,
                              PaymentService paymentService,
                              NotificationService notificationService,
                              UserProfileService userProfileService,
                              @Qualifier("publicRestTemplate") RestTemplate publicRestTemplate) {
        this.productService      = productService;
        this.cartService         = cartService;
        this.guestCartService    = guestCartService;
        this.orderService        = orderService;
        this.paymentService      = paymentService;
        this.notificationService = notificationService;
        this.userProfileService  = userProfileService;
        this.publicRestTemplate  = publicRestTemplate;
    }

    // ── API PROXY ─────────────────────────────────────────────────────────────

    @GetMapping("/api/products/suggestions")
    @ResponseBody
    public List<?> productSuggestionsProxy(@RequestParam(defaultValue = "") String q) {
        return productService.getSuggestions(q);
    }

    // ── HOME ──────────────────────────────────────────────────────────────────

    @GetMapping({"/", "/home"})
    public String home(Model model) {
        log.info("Welcome home");
        model.addAttribute(DATA, new HomePageModel(
                productService.getFeaturedProducts(),
                productService.getTopRatedProducts(8),
                productService.getNewestProducts(4),
                0));
        return "pages/home";
    }

    // ── REGISTER ──────────────────────────────────────────────────────────────

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute(DATA, new RegisterPageModel(null, null));
        return "pages/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username, @RequestParam String password,
                           @RequestParam String confirmPassword,
                           @RequestParam(required = false) String email,
                           @RequestParam(required = false) String fullName, Model model) {
        if (!password.equals(confirmPassword)) {
            model.addAttribute(DATA, new RegisterPageModel("Şifreler eşleşmiyor!", null));
            return "pages/register";
        }
        if (username.isBlank() || username.length() < 3) {
            model.addAttribute(DATA, new RegisterPageModel("Kullanıcı adı en az 3 karakter.", null));
            return "pages/register";
        }
        if (password.length() < 6) {
            model.addAttribute(DATA, new RegisterPageModel("Şifre en az 6 karakter.", null));
            return "pages/register";
        }
        try {
            var req = new RegisterRequest(
                    username.trim(), password,
                    email    != null && !email.isBlank()    ? email.trim()    : null,
                    fullName != null && !fullName.isBlank() ? fullName.trim() : null);
            publicRestTemplate.postForObject(gatewayUrl + "/register", req, Object.class);
            model.addAttribute(DATA, new RegisterPageModel(null, true));
        } catch (HttpClientErrorException e) {
            String msg = e.getStatusCode() == HttpStatus.CONFLICT
                    ? "Bu kullanıcı adı veya e-posta zaten kayıtlı."
                    : "Kayıt hatası: HTTP " + e.getStatusCode().value();
            model.addAttribute(DATA, new RegisterPageModel(msg, null));
        } catch (Exception e) {
            model.addAttribute(DATA, new RegisterPageModel("Kayıt hatası: " + e.getMessage(), null));
        }
        return "pages/register";
    }

    // ── DASHBOARD ─────────────────────────────────────────────────────────────

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal OidcUser user, Model model) {
        List<?> orders   = orderService.getOrders();
        List<?> payments = paymentService.getPayments();
        model.addAttribute(DATA, new DashboardPageModel(
                resolveUsername(user),
                user.getEmail(),
                orders.size(),
                payments.size(),
                orders.stream().limit(3).toList(),
                notificationService.getUnreadCount(),
                cartService.getCartCount(),
                productService.getTopRatedProducts(4)));
        return "pages/dashboard";
    }

    // ── PRODUCTS ──────────────────────────────────────────────────────────────

    @GetMapping("/products")
    public String products(@AuthenticationPrincipal OidcUser user, Model model,
                           HttpServletRequest request, HttpServletResponse response,
                           @RequestParam(required = false) String search,
                           @RequestParam(required = false) String category,
                           @RequestParam(required = false) String sort,
                           @RequestParam(required = false) String minPrice,
                           @RequestParam(required = false) String maxPrice,
                           @RequestParam(required = false) String success,
                           @RequestParam(required = false) String error) {
        List<?> products;
        String  loadError = null;
        try {
            products = productService.searchProducts(search, category, sort, minPrice, maxPrice);
        } catch (Exception e) {
            products  = List.of();
            loadError = "Ürünler yüklenemedi: " + extractMessage(e);
        }
        // Add to cart → return to the same page (including filters)
        String returnTo = request.getRequestURI()
                + (request.getQueryString() != null ? "?" + request.getQueryString() : "");
        model.addAttribute("returnTo", returnTo);
        model.addAttribute(DATA, new ProductsPageModel(
                products, search, category, sort, minPrice, maxPrice,
                resolveCartCount(user, request, response), 0L,
                error != null ? error : loadError,
                success));
        return "pages/products";
    }

    // ── PRODUCT DETAIL ────────────────────────────────────────────────────────

    @GetMapping("/products/{id}")
    public String productDetail(@PathVariable Long id,
                                @AuthenticationPrincipal OidcUser user,
                                HttpServletRequest request, HttpServletResponse response,
                                Model model) {
        try {
            Map<?, ?> product = productService.getProduct(id);
            if (product == null) return "redirect:/products?error=Ürün+bulunamadı";
            model.addAttribute(DATA, new ProductDetailPageModel(
                    product,
                    productService.getRelatedProducts(id, 4),
                    resolveCartCount(user, request, response), 0L, null));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 404) return "redirect:/products?error=Ürün+bulunamadı";
            return "redirect:/products?error=Ürün+yüklenemedi";
        } catch (Exception e) {
            return "redirect:/products?error=" + encode("Hata: " + extractMessage(e));
        }
        return "pages/product-detail";
    }

    // ── CART ──────────────────────────────────────────────────────────────────

    @GetMapping("/cart")
    public String cart(@AuthenticationPrincipal OidcUser user, Model model,
                       HttpServletRequest request, HttpServletResponse response,
                       @RequestParam(required = false) String success,
                       @RequestParam(required = false) String error) {
        List<?> items     = List.of();
        Object  total     = "0.00";
        int     count     = 0;
        String  loadError = null;

        if (user == null) {
            List<Map<String, Object>> guestItems =
                    guestCartService.buildViewItems(request, response, productService);
            items = guestItems;
            total = guestCartService.calcTotal(guestItems);
            count = guestItems.size();
        } else {
            try {
                CartResponse cartData = cartService.getCart();
                if (cartData != null) {
                    items = cartData.items();
                    total = cartData.total();
                    count = cartData.itemCount();
                }
            } catch (Exception e) {
                loadError = "Sepet yüklenemedi: " + extractMessage(e);
            }
        }
        model.addAttribute(DATA, new CartPageModel(
                items, total, count, 0L,
                error != null ? error : loadError, success));
        return "pages/cart";
    }

    @PostMapping("/cart/add")
    public String addToCart(@AuthenticationPrincipal OidcUser user,
                            HttpServletRequest request,
                            HttpServletResponse response,
                            @RequestParam Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            @RequestParam(required = false) String returnTo) {
        String dest = (returnTo != null && !returnTo.isBlank()) ? returnTo : "/products";
        String sep  = dest.contains("?") ? "&" : "?";
        try {
            if (user == null) {
                guestCartService.addItem(request, response, productId, quantity);
            } else {
                cartService.addItem(new AddItemRequest(productId, quantity));
            }
            return "redirect:" + dest + sep + "success=" + encode("Ürün sepete eklendi! 🛒");
        } catch (Exception e) {
            return "redirect:" + dest + sep + "error=" + encode("Sepete eklenemedi: " + extractMessage(e));
        }
    }

    @PostMapping("/cart/update")
    public String updateCart(@AuthenticationPrincipal OidcUser user,
                             HttpServletRequest request, HttpServletResponse response,
                             @RequestParam Long productId,
                             @RequestParam Integer quantity) {
        try {
            if (user == null) guestCartService.updateItem(request, response, productId, quantity);
            else              cartService.updateItem(productId, quantity);
        } catch (Exception e) { log.warn("Cart update failed: {}", e.getMessage()); }
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@AuthenticationPrincipal OidcUser user,
                                 HttpServletRequest request, HttpServletResponse response,
                                 @RequestParam Long productId) {
        try {
            if (user == null) guestCartService.removeItem(request, response, productId);
            else              cartService.removeItem(productId);
        } catch (Exception e) { log.warn("Remove from cart failed: {}", e.getMessage()); }
        return "redirect:/cart";
    }

    @PostMapping("/cart/clear")
    public String clearCart(@AuthenticationPrincipal OidcUser user,
                            HttpServletRequest request, HttpServletResponse response) {
        try {
            if (user == null) guestCartService.clear(request, response);
            else              cartService.clearCart();
        } catch (Exception e) { log.warn("Cart clear failed: {}", e.getMessage()); }
        return "redirect:/cart";
    }

    // ── CHECKOUT ──────────────────────────────────────────────────────────────

    @GetMapping("/checkout")
    public String checkout(@AuthenticationPrincipal OidcUser user, Model model) {
        try {
            CartResponse cartData = cartService.getCart();
            List<?> items = cartData != null ? cartData.items() : List.of();
            if (items.isEmpty()) return "redirect:/cart?error=Sepetiniz+boş";
            model.addAttribute(DATA, new CheckoutPageModel(
                    items, cartData != null ? cartData.total() : "0.00",
                    items.size(), 0L, null));
        } catch (Exception e) {
            model.addAttribute(DATA, new CheckoutPageModel(
                    List.of(), "0.00", 0, 0L, "Sepet yüklenemedi: " + extractMessage(e)));
        }
        return "pages/checkout";
    }

    @PostMapping("/checkout/pay")
    public void pay(@RequestParam(defaultValue = "CREDIT_CARD") String paymentMethod,
                    @AuthenticationPrincipal OidcUser user,
                    jakarta.servlet.http.HttpServletResponse response) throws java.io.IOException {
        String redirectTo;
        try {
            CartResponse cartData = cartService.getCart();
            if (cartData == null) { response.sendRedirect("/cart?error=Sepet+boş"); return; }

            List<OrderItemRequest> orderItems = new ArrayList<>();
            for (Object item : cartData.items()) {
                @SuppressWarnings("unchecked") Map<String, Object> i = (Map<String, Object>) item;
                orderItems.add(new OrderItemRequest(
                        Long.valueOf(i.get("productId").toString()),
                        Integer.valueOf(i.get("quantity").toString())));
            }

            List<?> orders = orderService.createFromCart(new CreateFromCartRequest(orderItems));
            if (orders == null || orders.isEmpty()) { response.sendRedirect("/cart?error=Sipariş+oluşturulamadı"); return; }

            @SuppressWarnings("unchecked") Map<String, Object> firstOrder = (Map<String, Object>) orders.get(0);
            Long orderId = Long.valueOf(firstOrder.get("id").toString());

            PaymentInitiateResponse payResult = paymentService.initiatePayment(
                    new InitiatePaymentRequest(orderId, cartData.total().toPlainString(), paymentMethod));

            if (payResult == null) { response.sendRedirect("/cart?error=Ödeme+başlatılamadı"); return; }

            try { cartService.clearCart(); } catch (Exception ignored) {}

            if ("CHALLENGE".equals(payResult.type()) && payResult.acsUrl() != null) {
                response.setStatus(303);
                response.setHeader("Location", payResult.acsUrl());
                return;
            }
            redirectTo = "/payment/result?paymentId=" + payResult.paymentId() + "&status=SUCCESS&type=FRICTIONLESS";
        } catch (Exception e) {
            log.error("Checkout pay failed: {}", e.getMessage());
            redirectTo = "/cart?error=" + encode("Ödeme hatası: " + extractMessage(e));
        }
        response.sendRedirect(redirectTo);
    }

    @GetMapping("/payment/result")
    public String paymentResult(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String paymentId,
                                @RequestParam(required = false) String type,
                                @AuthenticationPrincipal OidcUser user, Model model) {
        model.addAttribute(DATA, new PaymentResultPageModel(status, paymentId, type, 0, 0L));
        return "pages/payment-result";
    }

    // ── ORDERS ────────────────────────────────────────────────────────────────

    @GetMapping("/orders")
    public String orders(@AuthenticationPrincipal OidcUser user, Model model,
                         @RequestParam(required = false) String success,
                         @RequestParam(required = false) String error) {
        List<java.util.LinkedHashMap<String, Object>> enriched = new ArrayList<>();
        String loadError = null;
        try {
            for (Object o : orderService.getOrders()) {
                @SuppressWarnings("unchecked")
                java.util.LinkedHashMap<String, Object> item = new java.util.LinkedHashMap<>((Map<String, Object>) o);
                if (item.get("productName") == null || item.get("productName").toString().isBlank())
                    item.put("productName", "Ürün #" + item.get("productId"));
                enriched.add(item);
            }
        } catch (Exception e) {
            loadError = "Siparişler yüklenemedi: " + extractMessage(e);
        }
        model.addAttribute(DATA, new OrdersPageModel(
                enriched, cartService.getCartCount(), 0L,
                error != null ? error : loadError, success));
        return "pages/orders";
    }

    @GetMapping("/orders/{id}")
    public String orderDetail(@PathVariable Long id, @AuthenticationPrincipal OidcUser user, Model model) {
        try {
            Map<?, ?> order = orderService.getOrder(id);
            if (order == null) return "redirect:/orders?error=Sipariş+bulunamadı";
            @SuppressWarnings("unchecked")
            java.util.LinkedHashMap<String, Object> enriched = new java.util.LinkedHashMap<>((Map<String, Object>) order);
            if (enriched.get("productName") == null || enriched.get("productName").toString().isBlank())
                enriched.put("productName", "Ürün #" + enriched.get("productId"));
            Map<?, ?> product = Map.of();
            try {
                Map<?, ?> p = productService.getProduct((Long) Long.valueOf(order.get("productId").toString()));
                if (p != null) product = p;
            } catch (Exception ignored) {}
            model.addAttribute(DATA, new OrderDetailPageModel(enriched, product, cartService.getCartCount(), 0L, null));
        } catch (HttpClientErrorException e) {
            if (e.getStatusCode().value() == 403) return "redirect:/orders?error=Bu+siparişe+erişim+yetkiniz+yok";
            return "redirect:/orders?error=Sipariş+bulunamadı";
        } catch (Exception e) {
            return "redirect:/orders?error=" + encode("Hata: " + extractMessage(e));
        }
        return "pages/order-detail";
    }

    @PostMapping("/orders")
    public String createOrder(@RequestParam Long productId, @RequestParam Integer quantity) {
        try { orderService.createOrder(productId, quantity); }
        catch (Exception e) { log.warn("Order creation failed: {}", e.getMessage()); }
        return "redirect:/orders";
    }

    @PostMapping("/orders/{id}/cancel")
    public String cancelOrder(@PathVariable Long id) {
        try {
            orderService.cancelOrder(id);
            return "redirect:/orders?success=Sipariş+iptal+edildi";
        } catch (Exception e) {
            return "redirect:/orders/" + id + "?error=" + encode("İptal edilemedi: " + extractMessage(e));
        }
    }

    // ── PAYMENTS ──────────────────────────────────────────────────────────────

    @GetMapping("/payments")
    public String payments(@AuthenticationPrincipal OidcUser user, Model model) {
        String loadError = null;
        List<?> payments = List.of();
        try {
            payments = paymentService.getPayments();
        } catch (Exception e) {
            loadError = "Ödemeler yüklenemedi: " + extractMessage(e);
        }
        model.addAttribute(DATA, new PaymentsPageModel(payments, cartService.getCartCount(), 0L, loadError));
        return "pages/payments";
    }

    // ── PROFILE ───────────────────────────────────────────────────────────────

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal OidcUser user, Model model) {
        Map<?, ?> profile = Map.of();
        try {
            Map<?, ?> result = userProfileService.getProfile();
            if (result != null) profile = result;
        } catch (Exception ignored) {}
        model.addAttribute(DATA, new ProfilePageModel(
                profile, resolveUsername(user), user.getEmail(),
                cartService.getCartCount(), 0L, null));
        return "pages/profile";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(required = false) String fullName,
                                @RequestParam(required = false) String bio,
                                @RequestParam(required = false) String avatarUrl) {
        try {
            userProfileService.updateProfile(Map.of(
                    "fullName",  fullName  != null && !fullName.isBlank()  ? fullName  : "",
                    "bio",       bio       != null && !bio.isBlank()       ? bio       : "",
                    "avatarUrl", avatarUrl != null && !avatarUrl.isBlank() ? avatarUrl : ""));
        } catch (Exception e) { log.warn("Profile update failed: {}", e.getMessage()); }
        return "redirect:/profile";
    }

    // ── NOTIFICATIONS ─────────────────────────────────────────────────────────

    @GetMapping("/notifications")
    public String notifications(@AuthenticationPrincipal OidcUser user, Model model) {
        List<?> notifs    = List.of();
        long    unread    = 0L;
        String  loadError = null;
        try {
            notifs = notificationService.getNotifications();
            unread = notificationService.getUnreadCount();
        } catch (Exception e) {
            loadError = "Bildirimler yüklenemedi: " + extractMessage(e);
        }
        model.addAttribute(DATA, new NotificationsPageModel(notifs, unread, cartService.getCartCount(), loadError));
        return "pages/notifications";
    }

    @PostMapping("/notifications/read-all")
    public String markAllRead() {
        try { notificationService.markAllRead(); }
        catch (Exception e) { log.warn("Mark all read failed: {}", e.getMessage()); }
        return "redirect:/notifications";
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Giriş yapmış kullanıcı için cart-service'ten, misafir için session'dan sepet sayısı döner. */
    private int resolveCartCount(OidcUser user, HttpServletRequest request, HttpServletResponse response) {
        if (user == null) return guestCartService.getCount(request, response);
        try { return cartService.getCartCount(); } catch (Exception e) { return 0; }
    }

    private String resolveUsername(OidcUser user) {
        if (user.getPreferredUsername() != null) return user.getPreferredUsername();
        if (user.getGivenName()         != null) return user.getGivenName();
        return user.getSubject();
    }

    private String extractMessage(Exception e) {
        if (e instanceof HttpClientErrorException hce) return "HTTP " + hce.getStatusCode().value();
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }

    private String encode(String s) {
        return java.net.URLEncoder.encode(s, java.nio.charset.StandardCharsets.UTF_8);
    }
}
