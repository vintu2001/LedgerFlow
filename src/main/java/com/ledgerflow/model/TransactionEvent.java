package com.ledgerflow.model;

import java.time.Instant;

public record TransactionEvent(
    String transactionId,
    String accountId,
    String entryType,
    long amountCents,
    String currency,
    String metadata,
    Instant timestamp
) {
    public TransactionEvent {
        if (amountCents <= 0)
            throw new IllegalArgumentException("Amount must be positive: " + amountCents);
        if (transactionId == null || transactionId.isBlank())
            throw new IllegalArgumentException("transactionId is required");
        if (accountId == null || accountId.isBlank())
            throw new IllegalArgumentException("accountId is required");
        if (!isValidEntryType(entryType))
            throw new IllegalArgumentException("Invalid entryType: " + entryType);
        if (currency == null) currency = "USD";
        if (timestamp == null) timestamp = Instant.now();
    }

    private static boolean isValidEntryType(String type) {
        return type != null && switch (type) {
            case "charge", "credit", "refund", "adjustment" -> true;
            default -> false;
        };
    }
}
