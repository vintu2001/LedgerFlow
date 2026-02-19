package com.ledgerflow.service;

import com.ledgerflow.api.dto.BalanceResponse;
import com.ledgerflow.api.dto.StatementResponse;
import com.ledgerflow.repository.BalanceRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class QueryService {

    private final BalanceRepository balanceRepo;
    private final JdbcTemplate jdbc;

    public QueryService(BalanceRepository balanceRepo, JdbcTemplate jdbc) {
        this.balanceRepo = balanceRepo;
        this.jdbc = jdbc;
    }

    public BalanceResponse getBalance(String accountId) {
        return balanceRepo.getBalanceSummary(accountId)
            .map(s -> new BalanceResponse(
                s.accountId(),
                s.balanceCents(),
                s.totalCharges(),
                s.totalCredits(),
                s.totalRefunds(),
                s.transactionCount(),
                s.updatedAt()
            ))
            .orElse(new BalanceResponse(accountId, 0L, 0L, 0L, 0L, 0, null));
    }

    public List<StatementResponse> getStatement(String accountId, Instant from, Instant to) {
        return jdbc.query("""
            SELECT transaction_id, entry_type, amount_cents, currency, metadata, created_at
            FROM ledger_entries
            WHERE account_id = ?
              AND created_at >= ?
              AND created_at < ?
            ORDER BY created_at DESC
            LIMIT 1000
            """,
            (rs, rowNum) -> new StatementResponse(
                rs.getString("transaction_id"),
                accountId,
                rs.getString("entry_type"),
                rs.getLong("amount_cents"),
                rs.getString("currency"),
                rs.getString("metadata"),
                rs.getTimestamp("created_at").toInstant()
            ),
            accountId, Timestamp.from(from), Timestamp.from(to)
        );
    }

    public List<Map<String, Object>> getBillingReport(String accountId, Instant from, Instant to) {
        return jdbc.queryForList("""
            SELECT entry_type,
                   COUNT(*) as transaction_count,
                   SUM(amount_cents) as total_cents,
                   MIN(amount_cents) as min_cents,
                   MAX(amount_cents) as max_cents,
                   AVG(amount_cents)::BIGINT as avg_cents
            FROM ledger_entries
            WHERE account_id = ?
              AND created_at >= ?
              AND created_at < ?
            GROUP BY entry_type
            ORDER BY entry_type
            """,
            accountId, Timestamp.from(from), Timestamp.from(to)
        );
    }
}
