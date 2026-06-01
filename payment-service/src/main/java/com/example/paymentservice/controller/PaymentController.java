package com.example.paymentservice.controller;

import com.example.paymentservice.dto.*;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.service.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController @RequestMapping("/api/payments")
@Tag(name = "Payment Service", description = "Ödeme işlemleri")
public class PaymentController {

    private final PaymentService service;
    public PaymentController(PaymentService service) { this.service = service; }

    @Operation(summary = "3DS ödeme başlat — CHALLENGE veya FRICTIONLESS karar verir")
    @PostMapping("/initiate")
    public ResponseEntity<PaymentInitiateResponse> initiatePayment(
            @RequestBody InitiatePaymentRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.ok(service.initiatePayment(
                req.orderId(), jwt.getSubject(), req.amount(),
                req.paymentMethod() != null ? req.paymentMethod() : "CREDIT_CARD"));
    }

    @Operation(summary = "ACS'den gelen callback — 3DS sonucunu işle (JWT gerekmez)")
    @PostMapping("/3ds/callback")
    public ResponseEntity<Void> threeDsCallback(@RequestBody ThreeDsCallbackRequest req) {
        service.handle3dsCallback(req.acsTransactionId(), req.status());
        return ResponseEntity.ok().build();
    }

    @Operation(summary = "Ödeme durumunu sorgula", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/{id}")
    public Payment getById(@PathVariable Long id) { return service.findById(id); }

    @Operation(summary = "Ödeme başlat", security = @SecurityRequirement(name = "bearerAuth"))
    @PostMapping
    public ResponseEntity<Payment> createPayment(
            @RequestBody CreatePaymentRequest req,
            @AuthenticationPrincipal Jwt jwt) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
            service.createPayment(req.orderId(), jwt.getSubject(), req.amount(),
                    req.paymentMethod() != null ? req.paymentMethod() : "CREDIT_CARD"));
    }

    @Operation(summary = "Sipariş ödemesini getir", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/order/{orderId}")
    public Payment getByOrder(@PathVariable Long orderId) { return service.findByOrderId(orderId); }

    @Operation(summary = "Kendi ödemelerini listele", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping
    public List<Payment> myPayments(@AuthenticationPrincipal Jwt jwt) {
        return service.findByUserId(jwt.getSubject());
    }

    @Operation(summary = "Ödeme onayla (payments:manage — ADMIN veya MANAGER)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('SCOPE_payments:manage')")
    public Payment confirm(@PathVariable Long id) { return service.confirmPayment(id); }

    @Operation(summary = "İade et (payments:manage — ADMIN veya MANAGER)", security = @SecurityRequirement(name = "bearerAuth"))
    @PutMapping("/{id}/refund")
    @PreAuthorize("hasAuthority('SCOPE_payments:manage')")
    public Payment refund(@PathVariable Long id) { return service.refundPayment(id); }
}
