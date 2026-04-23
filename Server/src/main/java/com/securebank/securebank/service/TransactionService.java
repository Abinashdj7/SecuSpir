package com.securebank.securebank.service;

import com.securebank.securebank.dto.*;
import com.securebank.securebank.model.Account;
import com.securebank.securebank.model.Transaction;
import com.securebank.securebank.model.User;
import com.securebank.securebank.repo.AccountRepository;
import com.securebank.securebank.repo.TransactionRepository;
import com.securebank.securebank.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    // Get the currently logged-in user
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Verify the account belongs to the logged-in user
    private Account getOwnedAccount(Long accountId) {
        User user = getCurrentUser();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this account");
        }
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Account is not active");
        }
        return account;
    }

    // 💰 DEPOSIT
    @Transactional
    public TransactionResponse deposit(Long accountId, DepositRequest request) {
        Account account = getOwnedAccount(accountId);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .receiverAccount(account)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        return TransactionResponse.fromEntity(transactionRepository.save(transaction));
    }

    // 💸 WITHDRAW
    @Transactional
    public TransactionResponse withdraw(Long accountId, WithdrawRequest request) {
        Account account = getOwnedAccount(accountId);

        // Check sufficient balance
        if (account.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction transaction = Transaction.builder()
                .senderAccount(account)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.WITHDRAWAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        return TransactionResponse.fromEntity(transactionRepository.save(transaction));
    }

    // 🔁 TRANSFER
    @Transactional
    public TransactionResponse transfer(Long senderAccountId, TransferRequest request) {
        Account sender = getOwnedAccount(senderAccountId);

        // Find receiver by account number
        Account receiver = accountRepository
                .findByAccountNumber(request.getReceiverAccountNumber())
                .orElseThrow(() -> new RuntimeException("Receiver account not found"));

        if (receiver.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new RuntimeException("Receiver account is not active");
        }

        // Can't transfer to yourself
        if (sender.getAccountNumber().equals(receiver.getAccountNumber())) {
            throw new RuntimeException("Cannot transfer to the same account");
        }

        // Check sufficient balance
        if (sender.getBalance().compareTo(request.getAmount()) < 0) {
            throw new RuntimeException("Insufficient balance");
        }

        // Debit sender, credit receiver
        sender.setBalance(sender.getBalance().subtract(request.getAmount()));
        receiver.setBalance(receiver.getBalance().add(request.getAmount()));

        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction transaction = Transaction.builder()
                .senderAccount(sender)
                .receiverAccount(receiver)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        return TransactionResponse.fromEntity(transactionRepository.save(transaction));
    }

    // 📋 TRANSACTION HISTORY
    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(Long accountId) {
        getOwnedAccount(accountId); // security check
        return transactionRepository.findAllByAccountId(accountId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }
}