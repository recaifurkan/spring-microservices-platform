package com.example.acs.repository;
import com.example.acs.model.AcsTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface AcsTransactionRepository extends JpaRepository<AcsTransaction, Long> {
    Optional<AcsTransaction> findByTransactionId(String transactionId);
}

