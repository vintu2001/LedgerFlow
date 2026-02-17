package com.ledgerflow.api.dto;

import java.time.Instant;

public record BalanceResponse(
    String accountId,
    long balanceCents,
    long totalCharges,
    long totalCredits,
    long totalRefunds,
    int transactionCount,
    Instant updatedAt
) {}
