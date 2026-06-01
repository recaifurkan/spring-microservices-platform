package com.example.paymentservice.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity @Table(name = "payments")
@Getter @Setter @NoArgsConstructor
public class Payment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "user_id", nullable = false) private String userId;
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Enumerated(EnumType.STRING) private PaymentStatus status = PaymentStatus.PENDING;
    @Column(name = "payment_method") private String paymentMethod;
    @Column(name = "transaction_id") private String transactionId;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "updated_at") private LocalDateTime updatedAt = LocalDateTime.now();
    @Column(name = "three_ds_type") private String threeDsType;
    @Column(name = "acs_transaction_id") private String acsTransactionId;
    @Column(name = "three_ds_status") private String threeDsStatus;

    public enum PaymentStatus { PENDING, PENDING_3DS, SUCCESS, FAILED, REFUNDED }
}
