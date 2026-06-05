package com.securebank.securebank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.securebank.dto.CreateAccountRequest;
import com.securebank.securebank.model.Account;
import com.securebank.securebank.model.User;
import com.securebank.securebank.repo.AccountRepository;
import com.securebank.securebank.repo.TransactionRepository;
import com.securebank.securebank.repo.UserRepository;
import com.securebank.securebank.security.JwtUtil;
import com.securebank.securebank.security.LoginAttemptService;
import com.securebank.securebank.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class AccountIntegrationTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserDetailsServiceImpl userDetailsService;
    @Autowired LoginAttemptService loginAttemptService;

    private User userA;
    private User userB;
    private String tokenA;
    private String tokenB;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        loginAttemptService.resetAll();

        userA = userRepository.save(buildUser("a@test.com"));
        userB = userRepository.save(buildUser("b@test.com"));
        tokenA = generateToken(userA);
        tokenB = generateToken(userB);
    }

    // ── GET /api/accounts ─────────────────────────────────────────────────────

    @Test
    void getMyAccounts_unauthenticated_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMyAccounts_authenticated_returnsOnlyOwnAccounts() throws Exception {
        accountRepository.save(buildAccount(userA, BigDecimal.valueOf(500)));
        accountRepository.save(buildAccount(userA, BigDecimal.valueOf(200)));
        accountRepository.save(buildAccount(userB, BigDecimal.valueOf(100)));

        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void getMyAccounts_noAccounts_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/accounts").header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── POST /api/accounts ────────────────────────────────────────────────────

    @Test
    void createAccount_authenticated_persistsAndReturns() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(Account.AccountType.SAVINGS);

        mockMvc.perform(post("/api/accounts")
                        .header("Authorization", "Bearer " + tokenA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(0))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.accountNumber").isNotEmpty());

        assertThat(accountRepository.findByUserId(userA.getId())).hasSize(1);
    }

    @Test
    void createAccount_unauthenticated_returnsUnauthorized() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(Account.AccountType.CHECKING);

        mockMvc.perform(post("/api/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── GET /api/accounts/{id} ────────────────────────────────────────────────

    @Test
    void getAccountById_ownAccount_returnsAccount() throws Exception {
        Account account = accountRepository.save(buildAccount(userA, BigDecimal.valueOf(750)));

        mockMvc.perform(get("/api/accounts/" + account.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(account.getId()))
                .andExpect(jsonPath("$.balance").value(750));
    }

    @Test
    void getAccountById_otherUsersAccount_returnsForbidden() throws Exception {
        Account accountB = accountRepository.save(buildAccount(userB, BigDecimal.valueOf(100)));

        mockMvc.perform(get("/api/accounts/" + accountB.getId())
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAccountById_nonExistent_returnsNotFound() throws Exception {
        mockMvc.perform(get("/api/accounts/99999")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }

    // ── PATCH /api/accounts/{id}/freeze ───────────────────────────────────────

    @Test
    void freezeAccount_ownAccount_setsStatusToFrozen() throws Exception {
        Account account = accountRepository.save(buildAccount(userA, BigDecimal.ZERO));

        mockMvc.perform(patch("/api/accounts/" + account.getId() + "/freeze")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));

        Account updated = accountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getStatus()).isEqualTo(Account.AccountStatus.FROZEN);
    }

    @Test
    void freezeAccount_otherUsersAccount_returnsForbidden() throws Exception {
        Account accountB = accountRepository.save(buildAccount(userB, BigDecimal.ZERO));

        mockMvc.perform(patch("/api/accounts/" + accountB.getId() + "/freeze")
                        .header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isForbidden());

        Account unchanged = accountRepository.findById(accountB.getId()).orElseThrow();
        assertThat(unchanged.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private User buildUser(String email) {
        return User.builder()
                .firstName("Test").lastName("User")
                .email(email)
                .password(passwordEncoder.encode("Test1234!"))
                .role(User.Role.ROLE_USER)
                .build();
    }

    private Account buildAccount(User owner, BigDecimal balance) {
        return Account.builder()
                .user(owner)
                .accountNumber(UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase())
                .accountType(Account.AccountType.CHECKING)
                .balance(balance)
                .status(Account.AccountStatus.ACTIVE)
                .build();
    }

    private String generateToken(User user) {
        return jwtUtil.generateToken(userDetailsService.loadUserByUsername(user.getEmail()));
    }
}
