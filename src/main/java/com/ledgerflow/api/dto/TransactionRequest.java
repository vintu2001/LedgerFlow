package com.ledgerflow.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record TransactionRequest(
    @NotBlank String accountId,
    @NotBlank String entryType,
    @Positive long amountCents,
    String currency,
    String metadata
) {}
