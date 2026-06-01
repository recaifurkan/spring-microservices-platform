package com.example.acs.service;

import com.example.acs.dto.AcsCallbackRequest;
import com.example.acs.model.AcsTransaction;
import com.example.acs.repository.AcsTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.UUID;

@Service @Transactional
public class AcsService {

    private static final Logger log = LoggerFactory.getLogger(AcsService.class);
    private final AcsTransactionRepository repo;
    private final RestTemplate restTemplate = new RestTemplate();

    public AcsService(AcsTransactionRepository repo) { this.repo = repo; }

    /** Called by payment-service — creates a new 3DS transaction record */
    public AcsTransaction createTransaction(Long paymentId, String paymentCallbackUrl,
                                            String frontendCallbackUrl, String amount, String merchantName) {
        AcsTransaction txn = new AcsTransaction();
        txn.setTransactionId(UUID.randomUUID().toString());
        txn.setPaymentId(paymentId);
        txn.setPaymentCallbackUrl(paymentCallbackUrl);
        txn.setFrontendCallbackUrl(frontendCallbackUrl);
        txn.setAmount(amount);
        txn.setMerchantName(merchantName != null ? merchantName : "Microservice Demo Store");
        txn.setStatus(AcsTransaction.TxnStatus.PENDING);
        txn.setCreatedAt(LocalDateTime.now());
        txn.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        AcsTransaction saved = repo.save(txn);
        log.info("3DS transaction started: paymentId={} txnId={} amount={}", paymentId, saved.getTransactionId(), amount);
        return saved;
    }

    @Transactional(readOnly = true)
    public AcsTransaction getTransaction(String transactionId) {
        return repo.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    /** OTP verification — SUCCESS if correct, otherwise attempts++ → FAILED after 3 tries */
    public AcsTransaction verifyOtp(String transactionId, String otp) {
        AcsTransaction txn = getTransaction(transactionId);

        if (txn.getStatus() != AcsTransaction.TxnStatus.PENDING)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already completed: " + txn.getStatus());

        if (LocalDateTime.now().isAfter(txn.getExpiresAt())) {
            txn.setStatus(AcsTransaction.TxnStatus.EXPIRED);
            repo.save(txn);
            notifyPaymentService(txn, "FAILED");
            throw new ResponseStatusException(HttpStatus.GONE, "Transaction expired");
        }

        txn.setAttempts(txn.getAttempts() + 1);

        if (AcsTransaction.DEMO_OTP.equals(otp)) {
            txn.setStatus(AcsTransaction.TxnStatus.SUCCESS);
            repo.save(txn);
            log.info("OTP verified: txnId={} paymentId={} status=SUCCESS", transactionId, txn.getPaymentId());
            notifyPaymentService(txn, "SUCCESS");
        } else if (txn.getAttempts() >= 3) {
            txn.setStatus(AcsTransaction.TxnStatus.FAILED);
            repo.save(txn);
            log.info("OTP verification failed (max attempts exceeded): txnId={} paymentId={} attempts={}", transactionId, txn.getPaymentId(), txn.getAttempts());
            notifyPaymentService(txn, "FAILED");
        } else {
            repo.save(txn);
        }
        return txn;
    }

    /** Notify payment-service of the result */
    private void notifyPaymentService(AcsTransaction txn, String status) {
        if (txn.getPaymentCallbackUrl() == null) return;
        try {
            restTemplate.postForObject(
                    txn.getPaymentCallbackUrl(),
                    new AcsCallbackRequest(txn.getTransactionId(), status),
                    Void.class);
        } catch (Exception e) {
            log.warn("Payment service callback failed: {}", e.getMessage());
        }
    }
}

