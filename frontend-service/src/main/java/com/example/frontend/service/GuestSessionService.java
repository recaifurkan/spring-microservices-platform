package com.example.frontend.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
@RequiredArgsConstructor
public class GuestSessionService {

    public static final String OAUTH2_AUTH_REQUEST_COOKIE = "oauth2_auth_request";
    private static final int   COOKIE_MAX_AGE  = 7 * 24 * 3600; // 7 days

    private final GuestSessionStore store;


    // ── Guest ID management ─────────────────────────────────────────────────

    /** Reads the guest ID from the cookie; otherwise creates a new UUID and sets the cookie. */
    public String getOrCreateGuestId(HttpServletRequest request, HttpServletResponse response) {
        String id = readCookie(request, OAUTH2_AUTH_REQUEST_COOKIE);
        if (id == null || id.isBlank()) {
            id = UUID.randomUUID().toString();
            writeGuestIdCookie(response, id);
        }
        return id;
    }

    /** Reads the guest ID from the cookie; otherwise returns null (does not create a cookie). */
    public String getSessionId(HttpServletRequest request) {
        return readCookie(request, OAUTH2_AUTH_REQUEST_COOKIE);
    }

    public void clearGuestIdCookie(HttpServletResponse response) {
        Cookie c = new Cookie(OAUTH2_AUTH_REQUEST_COOKIE, "");
        c.setPath("/");
        c.setHttpOnly(true);
        c.setMaxAge(0);
        response.addCookie(c);
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
        Cookie c = new Cookie(OAUTH2_AUTH_REQUEST_COOKIE, id);
        c.setPath("/");
        c.setHttpOnly(true);
        c.setMaxAge(COOKIE_MAX_AGE);
        response.addCookie(c);
    }
}
