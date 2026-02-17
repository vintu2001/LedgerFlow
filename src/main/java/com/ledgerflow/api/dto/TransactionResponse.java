package com.ledgerflow.api.dto;

import java.time.Instant;

public record TransactionResponse(
    String transactionId,
    String status,
    Instant timestamp
) {}
