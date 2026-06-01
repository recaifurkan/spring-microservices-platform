package com.example.frontend.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Manages the guest (anonymous) user's cart.
 *
 * <p>How it works:
 * <ol>
 *   <li>When the guest adds an item to the cart for the first time, the browser receives
 *       a {@code GUEST_CART_ID=<UUID>} cookie.</li>
 *   <li>The cart data is stored in {@link GuestCartStore} (ConcurrentHashMap) by UUID.</li>
 *   <li>When the OAuth2 login callback arrives, the {@code GUEST_CART_ID} cookie is
 *       reliably sent from the browser (domain cookie, independent of session).</li>
 *   <li>{@code OAuth2LoginSuccessHandler} reads the UUID, transfers the cart to cart-service,
 *       and deletes the cookie.</li>
 * </ol>
 */
@Service
public class GuestCartService {

    public static final String GUEST_ID_COOKIE = "GUEST_CART_ID";
    private static final int   COOKIE_MAX_AGE  = 7 * 24 * 3600; // 7 days

    private final GuestCartStore store;

    public GuestCartService(GuestCartStore store) {
        this.store = store;
    }

    // ── Guest ID management ─────────────────────────────────────────────────

    /** Reads the guest ID from the cookie; otherwise creates a new UUID and sets the cookie. */
    public String getOrCreateGuestId(HttpServletRequest request, HttpServletResponse response) {
        String id = readCookie(request, GUEST_ID_COOKIE);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            writeGuestIdCookie(response, id);
        }
        return id;
    }

    /** Reads the guest ID from the cookie; otherwise returns null (does not create a cookie). */
    public String getGuestId(HttpServletRequest request) {
        return readCookie(request, GUEST_ID_COOKIE);
    }

    public void clearGuestIdCookie(HttpServletResponse response) {
        Cookie c = new Cookie(GUEST_ID_COOKIE, "");
        c.setPath("/");
        c.setHttpOnly(true);
        c.setMaxAge(0);
        response.addCookie(c);
    }

    // ── Read operations ──────────────────────────────────────────────────────

    public Map<Long, Integer> getItems(HttpServletRequest request, HttpServletResponse response) {
        return store.get(getOrCreateGuestId(request, response));
    }

    public int getCount(HttpServletRequest request, HttpServletResponse response) {
        String id = getGuestId(request);
        if (id == null) return 0;
        return store.get(id).values().stream().mapToInt(Integer::intValue).sum();
    }

    // ── Write operations ─────────────────────────────────────────────────────

    public void addItem(HttpServletRequest request, HttpServletResponse response,
                        Long productId, int quantity) {
        String id = getOrCreateGuestId(request, response);
        Map<Long, Integer> cart = store.get(id);
        cart.merge(productId, quantity, Integer::sum);
        store.save(id, cart);
    }

    public void updateItem(HttpServletRequest request, HttpServletResponse response,
                           Long productId, int quantity) {
        String id = getOrCreateGuestId(request, response);
        Map<Long, Integer> cart = store.get(id);
        if (quantity <= 0) cart.remove(productId);
        else               cart.put(productId, quantity);
        store.save(id, cart);
    }

    public void removeItem(HttpServletRequest request, HttpServletResponse response,
                           Long productId) {
        String id = getOrCreateGuestId(request, response);
        Map<Long, Integer> cart = store.get(id);
        cart.remove(productId);
        store.save(id, cart);
    }

    public void clear(HttpServletRequest request, HttpServletResponse response) {
        String id = getGuestId(request);
        if (id != null) store.remove(id);
        clearGuestIdCookie(response);
    }

    // ── Enrichment for view rendering ────────────────────────────────────────

    public List<Map<String, Object>> buildViewItems(HttpServletRequest request,
                                                    HttpServletResponse response,
                                                    ProductService productService) {
        return enrichItems(getItems(request, response), productService);
    }

    private List<Map<String, Object>> enrichItems(Map<Long, Integer> items,
                                                   ProductService productService) {
        List<Map<String, Object>> viewItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : items.entrySet()) {
            try {
                Map<?, ?> product = productService.getProduct(entry.getKey());
                if (product == null) continue;
                Object priceRaw = product.get("price");
                BigDecimal price = priceRaw instanceof BigDecimal bd
                        ? bd
                        : priceRaw != null ? new BigDecimal(priceRaw.toString()) : BigDecimal.ZERO;
                int qty = entry.getValue();
                Map<String, Object> view = new LinkedHashMap<>();
                view.put("productId",    entry.getKey());
                view.put("productName",  product.get("name"));
                view.put("productPrice", price);
                view.put("quantity",     qty);
                view.put("subtotal",     price.multiply(BigDecimal.valueOf(qty)));
                viewItems.add(view);
            } catch (Exception ignored) { }
        }
        return viewItems;
    }

    public BigDecimal calcTotal(List<Map<String, Object>> viewItems) {
        return viewItems.stream()
                .map(i -> (BigDecimal) i.get("subtotal"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private String readCookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private void writeGuestIdCookie(HttpServletResponse response, String id) {
        Cookie c = new Cookie(GUEST_ID_COOKIE, id);
        c.setPath("/");
        c.setHttpOnly(true);
        c.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(c);
    }
}
