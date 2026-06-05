package com.securebank.securebank.service;

import com.securebank.securebank.dto.*;
import com.securebank.securebank.model.Account;
import com.securebank.securebank.model.Transaction;
import com.securebank.securebank.repo.AccountRepository;
import com.securebank.securebank.repo.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final AccountService accountService;

    @Transactional
    public TransactionResponse deposit(Long accountId, DepositRequest request) {
        Account account = accountService.getOwnedAccountById(accountId);
        requireActive(account);

        account.setBalance(account.getBalance().add(request.getAmount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .receiverAccount(account)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        return TransactionResponse.fromEntity(transactionRepository.save(tx));
    }

    @Transactional
    public TransactionResponse withdraw(Long accountId, WithdrawRequest request) {
        Account account = accountService.getOwnedAccountById(accountId);
        requireActive(account);
        requireSufficientBalance(account, request.getAmount());

        account.setBalance(account.getBalance().subtract(request.getAmount()));
        accountRepository.save(account);

        Transaction tx = Transaction.builder()
                .senderAccount(account)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.WITHDRAWAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        return TransactionResponse.fromEntity(transactionRepository.save(tx));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TransactionResponse transfer(Long senderAccountId, TransferRequest request) {
        Account sender = accountService.getOwnedAccountById(senderAccountId);
        requireActive(sender);

        Account receiver = accountRepository
                .findByAccountNumber(request.getReceiverAccountNumber())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Receiver account not found"));

        if (receiver.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Receiver account is not active");
        }
        if (sender.getAccountNumber().equals(receiver.getAccountNumber())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Cannot transfer to the same account");
        }
        requireSufficientBalance(sender, request.getAmount());

        sender.setBalance(sender.getBalance().subtract(request.getAmount()));
        receiver.setBalance(receiver.getBalance().add(request.getAmount()));
        accountRepository.save(sender);
        accountRepository.save(receiver);

        Transaction tx = Transaction.builder()
                .senderAccount(sender)
                .receiverAccount(receiver)
                .amount(request.getAmount())
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description(request.getDescription())
                .build();

        return TransactionResponse.fromEntity(transactionRepository.save(tx));
    }

    @Transactional(readOnly = true)
    public List<TransactionResponse> getHistory(Long accountId) {
        accountService.getOwnedAccountById(accountId);
        return transactionRepository.findAllByAccountId(accountId)
                .stream()
                .map(TransactionResponse::fromEntity)
                .collect(Collectors.toList());
    }

    private void requireActive(Account account) {
        if (account.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Account is not active");
        }
    }

    private void requireSufficientBalance(Account account, java.math.BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Insufficient balance");
        }
    }
}
