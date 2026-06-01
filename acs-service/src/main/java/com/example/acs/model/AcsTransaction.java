package com.example.acs.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "acs_transactions")
@Getter @Setter @NoArgsConstructor
public class AcsTransaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "transaction_id", unique = true, nullable = false) private String transactionId;
    @Column(name = "payment_id") private Long paymentId;
    @Column(name = "payment_callback_url") private String paymentCallbackUrl;
    @Column(name = "frontend_callback_url") private String frontendCallbackUrl;
    @Column(name = "amount") private String amount;
    @Column(name = "merchant_name") private String merchantName;
    @Enumerated(EnumType.STRING) private TxnStatus status = TxnStatus.PENDING;
    @Column(name = "attempts") private int attempts = 0;
    @Column(name = "created_at") private LocalDateTime createdAt = LocalDateTime.now();
    @Column(name = "expires_at") private LocalDateTime expiresAt;

    public enum TxnStatus { PENDING, SUCCESS, FAILED, EXPIRED }

    public static final String DEMO_OTP = "1234";
}
