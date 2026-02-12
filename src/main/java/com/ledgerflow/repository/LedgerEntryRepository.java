package com.ledgerflow.repository;

import com.ledgerflow.model.LedgerEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, Long> {

    @Query("SELECT CASE WHEN COUNT(e) > 0 THEN true ELSE false END " +
           "FROM LedgerEntry e WHERE e.transactionId = :transactionId")
    boolean existsByTransactionId(String transactionId);

    List<LedgerEntry> findByAccountIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            String accountId, Instant from, Instant to);
}
