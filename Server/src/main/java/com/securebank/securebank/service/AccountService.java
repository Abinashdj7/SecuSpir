package com.securebank.securebank.service;

import com.securebank.securebank.dto.AccountResponse;
import com.securebank.securebank.dto.CreateAccountRequest;
import com.securebank.securebank.model.Account;
import com.securebank.securebank.model.User;
import com.securebank.securebank.repo.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final CurrentUserService currentUserService;

    public List<AccountResponse> getMyAccounts() {
        User user = currentUserService.getCurrentUser();
        return accountRepository.findByUserId(user.getId())
                .stream()
                .map(AccountResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public AccountResponse getAccountById(Long accountId) {
        return AccountResponse.fromEntity(getOwnedAccountById(accountId));
    }

    public AccountResponse createAccount(CreateAccountRequest request) {
        User user = currentUserService.getCurrentUser();
        Account account = buildAccount(user, request.getAccountType());
        accountRepository.save(account);
        return AccountResponse.fromEntity(account);
    }

    public Account createDefaultAccount(User user) {
        Account account = buildAccount(user, Account.AccountType.CHECKING);
        return accountRepository.save(account);
    }

    public AccountResponse freezeAccount(Long accountId) {
        Account account = getOwnedAccountById(accountId);
        account.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(account);
        return AccountResponse.fromEntity(account);
    }

    // Shared ownership guard used by both this service and TransactionService
    public Account getOwnedAccountById(Long accountId) {
        User user = currentUserService.getCurrentUser();
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Account not found"));
        if (!account.getUser().getId().equals(user.getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }
        return account;
    }

    private Account buildAccount(User user, Account.AccountType type) {
        return Account.builder()
                .user(user)
                .accountNumber(generateAccountNumber())
                .accountType(type)
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();
    }

    private String generateAccountNumber() {
        return UUID.randomUUID().toString().replace("-", "")
                .substring(0, 16).toUpperCase();
    }
}
