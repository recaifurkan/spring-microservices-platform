package com.example.acs.service;

import com.example.acs.model.AcsTransaction;
import com.example.acs.repository.AcsTransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcsServiceTest {

    @Mock AcsTransactionRepository repo;
    @InjectMocks AcsService service;

    private AcsTransaction txn;

    @BeforeEach
    void setUp() {
        txn = new AcsTransaction();
        txn.setTransactionId("txn-1");
        txn.setPaymentId(100L);
        txn.setPaymentCallbackUrl(null);
        txn.setFrontendCallbackUrl("http://frontend/callback");
        txn.setAmount("99.99");
        txn.setMerchantName("Demo Store");
        txn.setStatus(AcsTransaction.TxnStatus.PENDING);
        txn.setAttempts(0);
        txn.setExpiresAt(LocalDateTime.now().plusMinutes(5));
    }

    @Test
    void givenValidInputWhenCreatingTransactionThenPersistsPendingTxn() {
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcsTransaction result = service.createTransaction(100L, null, null, "99.99", null);

        assertThat(result.getTransactionId()).isNotBlank();
        assertThat(result.getStatus()).isEqualTo(AcsTransaction.TxnStatus.PENDING);
        assertThat(result.getMerchantName()).isEqualTo("Microservice Demo Store");
        verify(repo).save(any());
    }

    @Test
    void givenMissingTransactionWhenLoadingThenThrowsNotFound() {
        when(repo.findByTransactionId("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getTransaction("ghost"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void givenCorrectOtpWhenVerifyingThenMarksSuccess() {
        when(repo.findByTransactionId("txn-1")).thenReturn(Optional.of(txn));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcsTransaction result = service.verifyOtp("txn-1", AcsTransaction.DEMO_OTP);

        assertThat(result.getStatus()).isEqualTo(AcsTransaction.TxnStatus.SUCCESS);
        assertThat(result.getAttempts()).isEqualTo(1);
    }

    @Test
    void givenWrongOtpWhenAttemptsExhaustedThenMarksFailed() {
        txn.setAttempts(2);
        when(repo.findByTransactionId("txn-1")).thenReturn(Optional.of(txn));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        AcsTransaction result = service.verifyOtp("txn-1", "0000");

        assertThat(result.getStatus()).isEqualTo(AcsTransaction.TxnStatus.FAILED);
        assertThat(result.getAttempts()).isEqualTo(3);
    }

    @Test
    void givenExpiredTransactionWhenVerifyingThenThrowsGone() {
        txn.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        when(repo.findByTransactionId("txn-1")).thenReturn(Optional.of(txn));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThatThrownBy(() -> service.verifyOtp("txn-1", "0000"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.GONE);
    }

    @Test
    void givenCompletedTransactionWhenVerifyingThenThrowsConflict() {
        txn.setStatus(AcsTransaction.TxnStatus.SUCCESS);
        when(repo.findByTransactionId("txn-1")).thenReturn(Optional.of(txn));

        assertThatThrownBy(() -> service.verifyOtp("txn-1", "0000"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.CONFLICT);
    }
}

