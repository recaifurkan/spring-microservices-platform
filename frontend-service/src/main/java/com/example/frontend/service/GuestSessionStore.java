package com.example.frontend.service;

import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Guest sepetlerini bellekte tutan in-memory depo.
 * Anahtar: GUEST_CART_ID cookie değeri (UUID).
 */
@Component
public class GuestSessionStore {

    private final ConcurrentHashMap<String, OAuth2AuthorizationRequest> store =
            new ConcurrentHashMap<>();

    /** Varsa mevcut sepeti, yoksa yeni boş sepet döner. */
    public OAuth2AuthorizationRequest get(String guestId) {
        return store.get(guestId);
    }

    public void save(String guestId, OAuth2AuthorizationRequest cart) {
        if (cart == null ) store.remove(guestId);
        else store.put(guestId, cart);
    }

    /** Sepeti döner ve store'dan siler (login sonrası migration'da kullan). */
    public OAuth2AuthorizationRequest remove(String guestId) {
        return store.remove(guestId);
    }
}

