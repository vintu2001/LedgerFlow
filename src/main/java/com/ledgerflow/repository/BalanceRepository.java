package com.ledgerflow.repository;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public class BalanceRepository {

    private final JdbcTemplate jdbc;

    public BalanceRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void updateBalance(String accountId, long delta, String entryType,
                               long lastEntryId, String lastTransactionId) {
        String totalColumn = switch (entryType) {
            case "charge"  -> "total_charges = account_balances.total_charges + " + Math.abs(delta);
            case "credit"  -> "total_credits = account_balances.total_credits + " + Math.abs(delta);
            case "refund"  -> "total_refunds = account_balances.total_refunds + " + Math.abs(delta);
            default        -> "total_charges = account_balances.total_charges";
        };

        jdbc.update("""
            INSERT INTO account_balances
                (account_id, balance_cents, total_charges, total_credits, total_refunds,
                 transaction_count, last_entry_id, last_transaction_id, updated_at)
            VALUES (?, ?,
                    CASE WHEN ? = 'charge' THEN ? ELSE 0 END,
                    CASE WHEN ? = 'credit' THEN ? ELSE 0 END,
                    CASE WHEN ? = 'refund' THEN ? ELSE 0 END,
                    1, ?, ?, now())
            ON CONFLICT (account_id) DO UPDATE SET
                balance_cents = account_balances.balance_cents + EXCLUDED.balance_cents,
                %s,
                transaction_count = account_balances.transaction_count + 1,
                last_entry_id = EXCLUDED.last_entry_id,
                last_transaction_id = EXCLUDED.last_transaction_id,
                updated_at = now()
            """.formatted(totalColumn),
            accountId, delta,
            entryType, Math.abs(delta),
            entryType, Math.abs(delta),
            entryType, Math.abs(delta),
            lastEntryId, lastTransactionId
        );
    }

    public long getBalance(String accountId) {
        try {
            Long balance = jdbc.queryForObject(
                "SELECT balance_cents FROM account_balances WHERE account_id = ?",
                Long.class, accountId);
            return balance != null ? balance : 0L;
        } catch (EmptyResultDataAccessException e) {
            return 0L;
        }
    }

    public Optional<BalanceSummary> getBalanceSummary(String accountId) {
        return jdbc.query("""
            SELECT account_id, balance_cents, total_charges, total_credits,
                   total_refunds, transaction_count, updated_at
            FROM account_balances WHERE account_id = ?
            """,
            (rs, rowNum) -> new BalanceSummary(
                rs.getString("account_id"),
                rs.getLong("balance_cents"),
                rs.getLong("total_charges"),
                rs.getLong("total_credits"),
                rs.getLong("total_refunds"),
                rs.getInt("transaction_count"),
                rs.getTimestamp("updated_at").toInstant()
            ),
            accountId
        ).stream().findFirst();
    }

    public record BalanceSummary(
        String accountId, long balanceCents,
        long totalCharges, long totalCredits, long totalRefunds,
        int transactionCount, Instant updatedAt
    ) {}
}
