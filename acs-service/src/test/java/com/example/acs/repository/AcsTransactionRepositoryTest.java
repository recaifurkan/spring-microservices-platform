package com.example.acs.repository;

import com.example.acs.model.AcsTransaction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class AcsTransactionRepositoryTest {

    @Autowired AcsTransactionRepository repository;

    @Test
    void givenPersistedTransactionWhenQueryingThenFindsByTransactionId() {
        AcsTransaction txn = new AcsTransaction();
        txn.setTransactionId("txn-1");
        txn.setPaymentId(100L);
        txn.setExpiresAt(LocalDateTime.now().plusMinutes(5));
        repository.save(txn);

        assertThat(repository.findByTransactionId("txn-1")).isPresent();
    }
}

