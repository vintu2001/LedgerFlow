package com.ledgerflow.api;

import com.ledgerflow.api.dto.TransactionRequest;
import com.ledgerflow.api.dto.TransactionResponse;
import com.ledgerflow.service.TransactionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService transactionService;

    public TransactionController(TransactionService transactionService) {
        this.transactionService = transactionService;
    }

    @PostMapping
    public ResponseEntity<TransactionResponse> submitTransaction(
            @Valid @RequestBody TransactionRequest request) {
        TransactionResponse response = transactionService.submitTransaction(request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
