package com.example.paymentservice.service;

import com.example.paymentservice.client.AcsServiceClient;
import com.example.paymentservice.client.OrderServiceClient;
import com.example.paymentservice.dto.AcsInitRequest;
import com.example.paymentservice.dto.AcsInitResponse;
import com.example.paymentservice.dto.PaymentInitiateResponse;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service @Transactional
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);
    private final PaymentRepository repo;
    private final OrderServiceClient orderClient;
    private final AcsServiceClient acsClient;

    @Value("${payment-service.callback-base-url:http://localhost:8086}")
    private String callbackBaseUrl;
    @Value("${frontend-service.url:http://localhost:8070}")
    private String frontendUrl;
    @Value("${acs-service.url:http://localhost:8089}")
    private String acsServiceUrl;

    public PaymentService(PaymentRepository repo, OrderServiceClient orderClient, AcsServiceClient acsClient) {
        this.repo = repo; this.orderClient = orderClient; this.acsClient = acsClient;
    }

    @Transactional(readOnly = true)
    public Payment findByOrderId(Long orderId) {
        return repo.findByOrderId(orderId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ödeme bulunamadı: orderId=" + orderId));
    }

    @Transactional(readOnly = true)
    public List<Payment> findByUserId(String userId) { return repo.findByUserId(userId); }

    @Transactional(readOnly = true)
    public Payment findById(Long id) {
        return repo.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Ödeme bulunamadı: " + id));
    }

    public Payment createPayment(Long orderId, String userId, BigDecimal amount, String paymentMethod) {
        if (repo.findByOrderId(orderId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu sipariş için zaten ödeme mevcut");
        Payment payment = buildPayment(orderId, userId, amount, paymentMethod);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        Payment saved = repo.save(payment);
        log.info("Ödeme oluşturuldu (direkt): orderId={} userId={} amount={} method={} txnId={}",
                orderId, userId, amount, paymentMethod, saved.getTransactionId());
        confirmOrderStatus(orderId);
        return saved;
    }

    public PaymentInitiateResponse initiatePayment(Long orderId, String userId, BigDecimal amount, String paymentMethod) {
        if (repo.findByOrderId(orderId).isPresent())
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Bu sipariş için zaten ödeme mevcut");

        Payment payment = buildPayment(orderId, userId, amount, paymentMethod);
        payment.setStatus(Payment.PaymentStatus.PENDING_3DS);
        payment.setTransactionId("TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());

        boolean isChallenge = amount.compareTo(new BigDecimal("500")) >= 0;
        String threeDsType = isChallenge ? "CHALLENGE" : "FRICTIONLESS";
        payment.setThreeDsType(threeDsType);

        Payment saved = repo.save(payment);

        log.info("Ödeme başlatıldı: orderId={} userId={} amount={} 3dsType={} txnId={}",
                orderId, userId, amount, threeDsType, saved.getTransactionId());

        if (isChallenge) {
            String paymentCallbackUrl = callbackBaseUrl + "/api/payments/3ds/callback";
            String frontendCallbackUrl = frontendUrl + "/payment/result";
            try {
                AcsInitResponse acsResp = acsClient.init(new AcsInitRequest(
                        saved.getId(), paymentCallbackUrl, frontendCallbackUrl,
                        amount.toPlainString(), "Microservice Demo"));

                if (acsResp != null && acsResp.transactionId() != null) {
                    saved.setAcsTransactionId(acsResp.transactionId());
                    saved.setThreeDsStatus("PENDING");
                    repo.save(saved);
                    return new PaymentInitiateResponse(
                            saved.getId(), saved.getTransactionId(), amount,
                            "CHALLENGE", null,
                            acsResp.transactionId(),
                            acsServiceUrl + "/acs/challenge/" + acsResp.transactionId());
                }
            } catch (Exception e) {
                log.warn("ACS init failed, fallback FRICTIONLESS: {}", e.getMessage());
            }
            // ACS failed → frictionless fallback
            saved.setThreeDsType("FRICTIONLESS");
            saved.setStatus(Payment.PaymentStatus.SUCCESS);
            saved.setThreeDsStatus("SUCCESS");
            repo.save(saved);
            confirmOrderStatus(orderId);
            return new PaymentInitiateResponse(
                    saved.getId(), saved.getTransactionId(), amount,
                    "FRICTIONLESS", "SUCCESS", null, null);
        } else {
            saved.setThreeDsStatus("SUCCESS");
            saved.setStatus(Payment.PaymentStatus.SUCCESS);
            repo.save(saved);
            confirmOrderStatus(orderId);
            return new PaymentInitiateResponse(
                    saved.getId(), saved.getTransactionId(), amount,
                    "FRICTIONLESS", "SUCCESS", null, null);
        }
    }

    public Payment handle3dsCallback(String acsTransactionId, String status) {
        Payment payment = repo.findByAcsTransactionId(acsTransactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "3DS txn bulunamadı: " + acsTransactionId));
        payment.setThreeDsStatus(status);
        payment.setStatus("SUCCESS".equals(status) ? Payment.PaymentStatus.SUCCESS : Payment.PaymentStatus.FAILED);
        payment.setUpdatedAt(LocalDateTime.now());
        Payment saved = repo.save(payment);
        log.info("3DS callback işlendi: acsTransactionId={} status={} paymentId={} orderId={}",
                acsTransactionId, status, saved.getId(), saved.getOrderId());
        if (Payment.PaymentStatus.SUCCESS.equals(saved.getStatus())) {
            confirmOrderStatus(saved.getOrderId());
        }
        return saved;
    }

    public Payment confirmPayment(Long id) {
        Payment payment = findById(id);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setUpdatedAt(LocalDateTime.now());
        return repo.save(payment);
    }

    public Payment refundPayment(Long id) {
        Payment payment = findById(id);
        if (payment.getStatus() != Payment.PaymentStatus.SUCCESS)
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Sadece başarılı ödemeler iade edilebilir");
        payment.setStatus(Payment.PaymentStatus.REFUNDED);
        payment.setUpdatedAt(LocalDateTime.now());
        return repo.save(payment);
    }

    private Payment buildPayment(Long orderId, String userId, BigDecimal amount, String paymentMethod) {
        Payment p = new Payment();
        p.setOrderId(orderId); p.setUserId(userId); p.setAmount(amount);
        p.setPaymentMethod(paymentMethod); p.setStatus(Payment.PaymentStatus.PENDING);
        p.setCreatedAt(LocalDateTime.now()); p.setUpdatedAt(LocalDateTime.now());
        return p;
    }

    private void confirmOrderStatus(Long orderId) {
        try {
            orderClient.confirmPayment(orderId);
            log.info("Order {} CONFIRMED after successful payment", orderId);
        } catch (Exception e) {
            log.warn("Could not confirm order {}: {}", orderId, e.getMessage());
        }
    }
}
