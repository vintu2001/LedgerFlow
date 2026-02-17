package com.ledgerflow.api.dto;

import java.time.Instant;

public record StatementResponse(
    String transactionId,
    String accountId,
    String entryType,
    long amountCents,
    String currency,
    String metadata,
    Instant createdAt
) {}
