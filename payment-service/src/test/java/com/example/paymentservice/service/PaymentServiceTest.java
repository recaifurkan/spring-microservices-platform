package com.example.paymentservice.service;
import com.example.paymentservice.model.Payment;
import com.example.paymentservice.repository.PaymentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    @Mock PaymentRepository repo;
    @InjectMocks PaymentService service;
    private Payment sample;

    @BeforeEach void setUp() {
        sample = new Payment();
        sample.setOrderId(1L); sample.setUserId("user-1");
        sample.setAmount(new BigDecimal("199.99"));
        sample.setStatus(Payment.PaymentStatus.SUCCESS);
    }

    @Test void findByOrderId_found() {
        when(repo.findByOrderId(1L)).thenReturn(Optional.of(sample));
        assertThat(service.findByOrderId(1L).getUserId()).isEqualTo("user-1");
    }

    @Test void findByOrderId_notFound_throws() {
        when(repo.findByOrderId(99L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.findByOrderId(99L)).isInstanceOf(ResponseStatusException.class);
    }

    @Test void findByUserId_returnsList() {
        when(repo.findByUserId("user-1")).thenReturn(List.of(sample));
        assertThat(service.findByUserId("user-1")).hasSize(1);
    }

    @Test void createPayment_success() {
        when(repo.findByOrderId(2L)).thenReturn(Optional.empty());
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Payment result = service.createPayment(2L, "user-1", new BigDecimal("99.99"), "CREDIT_CARD");
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.SUCCESS);
        assertThat(result.getTransactionId()).isNotNull();
    }

    @Test void createPayment_duplicate_throws() {
        when(repo.findByOrderId(1L)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> service.createPayment(1L, "user-1", BigDecimal.TEN, "CARD"))
            .isInstanceOf(ResponseStatusException.class);
    }

    @Test void refundPayment_success() {
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        Payment result = service.refundPayment(1L);
        assertThat(result.getStatus()).isEqualTo(Payment.PaymentStatus.REFUNDED);
    }

    @Test void refundPayment_notSuccess_throws() {
        sample.setStatus(Payment.PaymentStatus.PENDING);
        when(repo.findById(1L)).thenReturn(Optional.of(sample));
        assertThatThrownBy(() -> service.refundPayment(1L)).isInstanceOf(ResponseStatusException.class);
    }
}

