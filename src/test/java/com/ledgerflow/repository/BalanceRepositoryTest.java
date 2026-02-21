package com.ledgerflow.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BalanceRepositoryTest {

    @Mock private JdbcTemplate jdbc;
    @InjectMocks private BalanceRepository balanceRepo;

    @Test
    void updateBalanceExecutesUpsert() {
        when(jdbc.update(anyString(), any(Object[].class))).thenReturn(1);

        balanceRepo.updateBalance("acct-123", -1500L, "charge", 1L, "txn-001");

        verify(jdbc).update(
            argThatContains("ON CONFLICT"),
            eq("acct-123"), eq(-1500L),
            eq("charge"), eq(1500L),
            eq("charge"), eq(1500L),
            eq("charge"), eq(1500L),
            eq(1L), eq("txn-001")
        );
    }

    @Test
    void getBalanceReturnsStoredValue() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq("acct-123")))
            .thenReturn(5000L);

        long balance = balanceRepo.getBalance("acct-123");

        assertEquals(5000L, balance);
    }

    @Test
    void getBalanceReturnsZeroWhenNotFound() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq("acct-new")))
            .thenThrow(new EmptyResultDataAccessException(1));

        long balance = balanceRepo.getBalance("acct-new");

        assertEquals(0L, balance);
    }

    @Test
    void getBalanceReturnsZeroForNull() {
        when(jdbc.queryForObject(anyString(), eq(Long.class), eq("acct-null")))
            .thenReturn(null);

        long balance = balanceRepo.getBalance("acct-null");

        assertEquals(0L, balance);
    }

    private static String argThatContains(String substring) {
        return org.mockito.ArgumentMatchers.argThat(arg -> arg != null && arg.contains(substring));
    }
}
