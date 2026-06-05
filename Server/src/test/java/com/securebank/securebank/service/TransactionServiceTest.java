package com.securebank.securebank.service;

import com.securebank.securebank.dto.DepositRequest;
import com.securebank.securebank.dto.TransactionResponse;
import com.securebank.securebank.dto.TransferRequest;
import com.securebank.securebank.dto.WithdrawRequest;
import com.securebank.securebank.model.Account;
import com.securebank.securebank.model.Transaction;
import com.securebank.securebank.model.User;
import com.securebank.securebank.repo.AccountRepository;
import com.securebank.securebank.repo.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private TransactionRepository transactionRepository;
    @Mock private AccountRepository accountRepository;
    @Mock private AccountService accountService;

    @InjectMocks private TransactionService transactionService;

    private User testUser;
    private Account senderAccount;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("user@example.com").role(User.Role.ROLE_USER).build();
        senderAccount = Account.builder()
                .id(1L)
                .user(testUser)
                .accountNumber("SENDER1234567890")
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1000))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        when(accountService.getOwnedAccountById(1L)).thenReturn(senderAccount);
    }

    @Test
    void deposit_addsBalanceAndReturnsResponse() {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(500));
        request.setDescription("Salary");

        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            return Transaction.builder()
                    .id(1L).receiverAccount(t.getReceiverAccount()).amount(t.getAmount())
                    .type(t.getType()).status(t.getStatus()).description(t.getDescription()).build();
        });

        TransactionResponse result = transactionService.deposit(1L, request);

        assertThat(result.getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(result.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(500));
        assertThat(senderAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1500));
    }

    @Test
    void withdraw_subtractsBalanceAndReturnsResponse() {
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(BigDecimal.valueOf(300));
        request.setDescription("ATM");

        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            return Transaction.builder()
                    .id(1L).senderAccount(t.getSenderAccount()).amount(t.getAmount())
                    .type(t.getType()).status(t.getStatus()).description(t.getDescription()).build();
        });

        TransactionResponse result = transactionService.withdraw(1L, request);

        assertThat(result.getType()).isEqualTo(Transaction.TransactionType.WITHDRAWAL);
        assertThat(senderAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    void withdraw_insufficientBalance_throwsException() {
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(BigDecimal.valueOf(9999));

        assertThatThrownBy(() -> transactionService.withdraw(1L, request))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void transfer_debitsAndCreditsCorrectly() {
        Account receiver = Account.builder()
                .id(2L)
                .user(User.builder().id(2L).build())
                .accountNumber("RECV0000000000AB")
                .accountType(Account.AccountType.SAVINGS)
                .balance(BigDecimal.valueOf(200))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("RECV0000000000AB");
        request.setAmount(BigDecimal.valueOf(400));
        request.setDescription("Rent");

        when(accountRepository.findByAccountNumber("RECV0000000000AB")).thenReturn(Optional.of(receiver));
        when(accountRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(transactionRepository.save(any())).thenAnswer(inv -> {
            Transaction t = inv.getArgument(0);
            return Transaction.builder()
                    .id(1L).senderAccount(t.getSenderAccount()).receiverAccount(t.getReceiverAccount())
                    .amount(t.getAmount()).type(t.getType()).status(t.getStatus()).build();
        });

        TransactionResponse result = transactionService.transfer(1L, request);

        assertThat(result.getType()).isEqualTo(Transaction.TransactionType.TRANSFER);
        assertThat(senderAccount.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(receiver.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(600));
    }

    @Test
    void transfer_insufficientBalance_throwsException() {
        Account receiver = Account.builder()
                .id(2L).user(User.builder().id(2L).build())
                .accountNumber("RECV0000000000AB")
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("RECV0000000000AB");
        request.setAmount(BigDecimal.valueOf(9999));

        when(accountRepository.findByAccountNumber("RECV0000000000AB")).thenReturn(Optional.of(receiver));

        assertThatThrownBy(() -> transactionService.transfer(1L, request))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void transfer_sameAccount_throwsException() {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("SENDER1234567890");
        request.setAmount(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("SENDER1234567890")).thenReturn(Optional.of(senderAccount));

        assertThatThrownBy(() -> transactionService.transfer(1L, request))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Cannot transfer to the same account");
    }

    @Test
    void transfer_frozenReceiver_throwsException() {
        Account frozenReceiver = Account.builder()
                .id(2L).user(User.builder().id(2L).build())
                .accountNumber("RECV0000000000AB")
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.FROZEN)
                .build();

        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("RECV0000000000AB");
        request.setAmount(BigDecimal.valueOf(100));

        when(accountRepository.findByAccountNumber("RECV0000000000AB")).thenReturn(Optional.of(frozenReceiver));

        assertThatThrownBy(() -> transactionService.transfer(1L, request))
                .isInstanceOf(org.springframework.web.server.ResponseStatusException.class)
                .hasMessageContaining("Receiver account is not active");
    }

    @Test
    void getHistory_returnsTransactionsForAccount() {
        Transaction t = Transaction.builder()
                .id(1L)
                .receiverAccount(senderAccount)
                .amount(BigDecimal.valueOf(100))
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        when(transactionRepository.findAllByAccountId(1L)).thenReturn(List.of(t));

        List<TransactionResponse> result = transactionService.getHistory(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(Transaction.TransactionType.DEPOSIT);
        assertThat(result.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100));
    }
}
