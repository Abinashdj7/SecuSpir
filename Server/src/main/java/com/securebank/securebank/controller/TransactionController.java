package com.securebank.securebank.controller;

import com.securebank.securebank.dto.*;
import com.securebank.securebank.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts/{accountId}/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    // GET /api/accounts/{accountId}/transactions
    @GetMapping
    public ResponseEntity<List<TransactionResponse>> getHistory(
            @PathVariable Long accountId) {
        return ResponseEntity.ok(transactionService.getHistory(accountId));
    }

    // POST /api/accounts/{accountId}/transactions/deposit
    @PostMapping("/deposit")
    public ResponseEntity<TransactionResponse> deposit(
            @PathVariable Long accountId,
            @Valid @RequestBody DepositRequest request) {
        return ResponseEntity.ok(transactionService.deposit(accountId, request));
    }

    // POST /api/accounts/{accountId}/transactions/withdraw
    @PostMapping("/withdraw")
    public ResponseEntity<TransactionResponse> withdraw(
            @PathVariable Long accountId,
            @Valid @RequestBody WithdrawRequest request) {
        return ResponseEntity.ok(transactionService.withdraw(accountId, request));
    }

    // POST /api/accounts/{accountId}/transactions/transfer
    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            @PathVariable Long accountId,
            @Valid @RequestBody TransferRequest request) {
        return ResponseEntity.ok(transactionService.transfer(accountId, request));
    }
}