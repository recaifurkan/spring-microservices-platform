package com.example.orderservice.controller;

import com.example.orderservice.dto.CreateFromCartRequest;
import com.example.orderservice.dto.CreateOrderRequest;
import com.example.orderservice.model.Order;
import com.example.orderservice.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/orders")
@Tag(name = "Order Service", description = "Sipariş yönetimi")
public class OrderController {

    private final OrderService service;
    public OrderController(OrderService service) { this.service = service; }

    @Operation(summary = "Kendi siparişlerini listele", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public List<Order> myOrders(@AuthenticationPrincipal Jwt jwt) {
        return service.findByUserId(jwt.getSubject());
    }

    @Operation(summary = "Tüm siparişler (ROLE_ADMIN veya ROLE_MANAGER)", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public List<Order> allOrders() { return service.findAll(); }

    @Operation(summary = "Sipariş detayı", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public Order getById(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Sepetten sipariş oluştur", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping("/from-cart")
    public ResponseEntity<List<Order>> createFromCart(
            @RequestBody CreateFromCartRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createOrdersFromCart(jwt.getSubject(), req.items()));
    }

    @Operation(summary = "Sipariş oluştur", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<Order> create(@RequestBody CreateOrderRequest req,
                                        @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(service.createOrder(jwt.getSubject(), req.productId(), req.quantity()));
    }

    @Operation(summary = "Sipariş durumunu güncelle (ROLE_ADMIN veya ROLE_MANAGER)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public Order updateStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateStatus(id, Order.OrderStatus.valueOf(status.toUpperCase()));
    }

    @Operation(summary = "Siparişi iptal et", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/cancel")
    public Order cancel(@PathVariable Long id, @AuthenticationPrincipal Jwt jwt) {
        return service.cancelOrder(id, jwt.getSubject());
    }

    @Operation(summary = "Ödeme sonrası sipariş onayla (internal - payment-service çağırır)")
    @PostMapping("/{id}/confirm-payment")
    public Order confirmPayment(@PathVariable Long id,
                                @AuthenticationPrincipal Jwt jwt
    ) {
        return service.updateStatus(id, Order.OrderStatus.CONFIRMED);
    }
}
