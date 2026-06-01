package com.example.acs.controller;

import com.example.acs.dto.AcsInitRequest;
import com.example.acs.model.AcsTransaction;
import com.example.acs.service.AcsService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;

import java.math.BigDecimal;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AcsControllerTest {

    @Mock AcsService service;
    @InjectMocks AcsController controller;

    @Test
    void givenInitRequestWhenCreatingTransactionThenReturnsTransactionId() {
        AcsTransaction txn = new AcsTransaction();
        txn.setTransactionId("txn-1");
        when(service.createTransaction(1L, "http://pay", "http://front", "10.00", "Demo"))
                .thenReturn(txn);

        var response = controller.initTransaction(new AcsInitRequest(1L, "http://pay", "http://front", new BigDecimal("10.00").toPlainString(), "Demo"));

        assertThat(response.transactionId()).isEqualTo("txn-1");
    }

    @Test
    void givenExistingTransactionWhenChallengePageThenAddsTxnToModel() {
        AcsTransaction txn = new AcsTransaction();
        when(service.getTransaction("txn-1")).thenReturn(txn);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.challengePage("txn-1", model);

        assertThat(view).isEqualTo("challenge");
        assertThat(model.asMap()).containsEntry("txn", txn);
    }

    @Test
    void givenSuccessOtpWhenVerifyingThenRedirectsToResult() {
        AcsTransaction txn = new AcsTransaction();
        txn.setStatus(AcsTransaction.TxnStatus.SUCCESS);
        when(service.verifyOtp("txn-1", "1234")).thenReturn(txn);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.verifyOtp("txn-1", "1234", model);

        assertThat(view).isEqualTo("redirect:/acs/result/txn-1?status=SUCCESS");
    }

    @Test
    void givenWrongOtpWhenVerifyingThenReturnsChallengeWithError() {
        AcsTransaction txn = new AcsTransaction();
        txn.setStatus(AcsTransaction.TxnStatus.PENDING);
        txn.setAttempts(1);
        when(service.verifyOtp("txn-1", "0000")).thenReturn(txn);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.verifyOtp("txn-1", "0000", model);

        assertThat(view).isEqualTo("challenge");
        assertThat(model.asMap()).containsEntry("error", "Invalid OTP! You have 2 attempts left.");
    }

    @Test
    void givenFrontendCallbackWhenResultPageThenRedirectsToFrontend() {
        AcsTransaction txn = new AcsTransaction();
        txn.setPaymentId(99L);
        txn.setFrontendCallbackUrl("http://frontend/callback");
        when(service.getTransaction("txn-1")).thenReturn(txn);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.resultPage("txn-1", "SUCCESS", model);

        assertThat(view).isEqualTo("redirect:http://frontend/callback?paymentId=99&status=SUCCESS");
    }

    @Test
    void givenNoRedirectWhenResultPageThenReturnsResultView() {
        AcsTransaction txn = new AcsTransaction();
        when(service.getTransaction("txn-1")).thenReturn(txn);
        ExtendedModelMap model = new ExtendedModelMap();

        String view = controller.resultPage("txn-1", "FAILED", model);

        assertThat(view).isEqualTo("result");
        assertThat(model.asMap()).containsEntry("status", "FAILED");
    }

    @Test
    void givenHealthRequestWhenCallingThenReturnsUp() {
        assertThat(controller.health().getBody()).isEqualTo(Map.of("status", "UP", "service", "acs-service"));
    }
}

