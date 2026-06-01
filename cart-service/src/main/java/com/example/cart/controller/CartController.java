package com.example.cart.controller;

import com.example.cart.dto.*;
import com.example.cart.model.CartItem;
import com.example.cart.service.CartService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService service;
    public CartController(CartService service) { this.service = service; }

    @GetMapping
    public CartResponse getCart(@AuthenticationPrincipal Jwt jwt) {
        List<CartItem> items = service.getCart(jwt.getSubject());
        return new CartResponse(items, service.getTotal(jwt.getSubject()), items.size());
    }

    @GetMapping("/count")
    public CartCountResponse getCount(@AuthenticationPrincipal Jwt jwt) {
        return new CartCountResponse(service.getItemCount(jwt.getSubject()));
    }

    @PostMapping("/items")
    public CartItem addItem(@RequestBody AddItemRequest req, @AuthenticationPrincipal Jwt jwt) {
        int qty = req.quantity() != null ? req.quantity() : 1;
        return service.addItem(jwt.getSubject(), req.productId(), qty);
    }

    @PutMapping("/items/{productId}")
    public ResponseEntity<CartItem> updateItem(@PathVariable Long productId,
                                               @RequestBody UpdateQuantityRequest req,
                                               @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.updateQuantity(jwt.getSubject(), productId, req.quantity()));
    }

    @DeleteMapping("/items/{productId}")
    public ResponseEntity<Void> removeItem(@PathVariable Long productId,
                                           @AuthenticationPrincipal Jwt jwt) {
        service.removeItem(jwt.getSubject(), productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal Jwt jwt) {
        service.clearCart(jwt.getSubject());
        return ResponseEntity.noContent().build();
    }
}
