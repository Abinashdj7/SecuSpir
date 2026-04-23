package com.securebank.securebank.service;

import com.securebank.securebank.dto.AccountResponse;
import com.securebank.securebank.dto.CreateAccountRequest;
import com.securebank.securebank.model.Account;
import com.securebank.securebank.model.User;
import com.securebank.securebank.repo.AccountRepository;
import com.securebank.securebank.repo.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    // Get the currently authenticated user
    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext()
                .getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Get all accounts belonging to the logged-in user
    public List<AccountResponse> getMyAccounts() {
        User user = getCurrentUser();
        System.out.println("🔍 Getting accounts for user: " + user.getEmail() + " (ID: " + user.getId() + ")");

        List<Account> accounts = accountRepository.findByUserId(user.getId());
        System.out.println("📊 Found " + accounts.size() + " accounts for user");

        return accounts.stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    // Get a single account by ID (only if it belongs to the logged-in user)
    public AccountResponse getAccountById(Long accountId) {
        User user = getCurrentUser();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this account");
        }

        return AccountResponse.fromEntity(account);
    }

    // Open a new account for the logged-in user
    public AccountResponse createAccount(CreateAccountRequest request) {
        User user = getCurrentUser();

        Account account = Account.builder()
                .user(user)
                .accountNumber(generateAccountNumber())
                .accountType(request.getAccountType())
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        accountRepository.save(account);
        return AccountResponse.fromEntity(account);
    }

    // Freeze an account (admin or owner action)
    public AccountResponse freezeAccount(Long accountId) {
        User user = getCurrentUser();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new RuntimeException("Account not found"));

        if (!account.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied to this account");
        }

        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);
        return AccountResponse.fromEntity(account);
    }

    private String generateAccountNumber() {
        return UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase();
    }
}