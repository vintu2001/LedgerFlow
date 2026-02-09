package com.ledgerflow.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "account_balances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalance {

    @Id
    @Column(name = "account_id")
    private String accountId;

    @Column(name = "balance_cents", nullable = false)
    private long balanceCents;

    @Column(name = "total_charges", nullable = false)
    private long totalCharges;

    @Column(name = "total_credits", nullable = false)
    private long totalCredits;

    @Column(name = "total_refunds", nullable = false)
    private long totalRefunds;

    @Column(name = "transaction_count", nullable = false)
    private int transactionCount;

    @Column(name = "last_entry_id")
    private Long lastEntryId;

    @Column(name = "last_transaction_id")
    private String lastTransactionId;

    @Column(name = "updated_at")
    private Instant updatedAt;
}
