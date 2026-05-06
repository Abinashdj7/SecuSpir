package com.securebank.securebank.service;

import com.securebank.securebank.dto.AccountResponse;
import com.securebank.securebank.dto.CreateAccountRequest;
import com.securebank.securebank.model.Account;
import com.securebank.securebank.model.User;
import com.securebank.securebank.repo.AccountRepository;
import com.securebank.securebank.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock private AccountRepository accountRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private AccountService accountService;

    private User testUser;
    private Account testAccount;

    @BeforeEach
    void setUp() {
        testUser = User.builder().id(1L).email("user@example.com").role(User.Role.ROLE_USER).build();
        testAccount = Account.builder()
                .id(1L)
                .user(testUser)
                .accountNumber("ACC1234567890ABCD")
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1000))
                .status(Account.AccountStatus.ACTIVE)
                .build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("user@example.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getMyAccounts_returnsAccountsForCurrentUser() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findByUserId(1L)).thenReturn(List.of(testAccount));

        List<AccountResponse> result = accountService.getMyAccounts();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAccountNumber()).isEqualTo("ACC1234567890ABCD");
        assertThat(result.get(0).getBalance()).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    @Test
    void getAccountById_success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));

        AccountResponse result = accountService.getAccountById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getAccountType()).isEqualTo(Account.AccountType.CHECKING);
    }

    @Test
    void getAccountById_notFound_throwsException() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> accountService.getAccountById(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Account not found");
    }

    @Test
    void getAccountById_accessDenied_throwsException() {
        User otherUser = User.builder().id(2L).email("other@example.com").build();
        Account otherAccount = Account.builder()
                .id(5L)
                .user(otherUser)
                .accountNumber("OTHER0000000000AB")
                .accountType(Account.AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(5L)).thenReturn(Optional.of(otherAccount));

        assertThatThrownBy(() -> accountService.getAccountById(5L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Access denied to this account");
    }

    @Test
    void createAccount_success() {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(Account.AccountType.SAVINGS);

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse result = accountService.createAccount(request);

        assertThat(result.getAccountType()).isEqualTo(Account.AccountType.SAVINGS);
        assertThat(result.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
    }

    @Test
    void freezeAccount_success() {
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class))).thenAnswer(inv -> inv.getArgument(0));

        AccountResponse result = accountService.freezeAccount(1L);

        assertThat(result.getStatus()).isEqualTo(Account.AccountStatus.FROZEN);
    }

    @Test
    void freezeAccount_accessDenied_throwsException() {
        User otherUser = User.builder().id(2L).build();
        Account otherAccount = Account.builder()
                .id(1L)
                .user(otherUser)
                .accountNumber("OTHER0000000000AB")
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(testUser));
        when(accountRepository.findById(1L)).thenReturn(Optional.of(otherAccount));

        assertThatThrownBy(() -> accountService.freezeAccount(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Access denied to this account");
    }
}
