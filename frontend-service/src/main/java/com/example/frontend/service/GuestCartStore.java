package com.example.frontend.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for guest carts.
 * Key: GUEST_CART_ID cookie value (UUID).
 */
@Component
public class GuestCartStore {

    private final ConcurrentHashMap<String, LinkedHashMap<Long, Integer>> store =
            new ConcurrentHashMap<>();

    /** Returns the existing cart if present; otherwise creates a new empty cart. */
    public LinkedHashMap<Long, Integer> get(String guestId) {
        return store.computeIfAbsent(guestId, k -> new LinkedHashMap<>());
    }

    public void save(String guestId, Map<Long, Integer> cart) {
        if (cart == null || cart.isEmpty()) store.remove(guestId);
        else store.put(guestId, new LinkedHashMap<>(cart));
    }

    /** Returns the cart and removes it from the store (used during post-login migration). */
    public Map<Long, Integer> remove(String guestId) {
        Map<Long, Integer> cart = store.remove(guestId);
        return cart != null ? cart : new LinkedHashMap<>();
    }
}

