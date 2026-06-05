package com.securebank.securebank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.securebank.dto.DepositRequest;
import com.securebank.securebank.dto.TransferRequest;
import com.securebank.securebank.dto.WithdrawRequest;
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
class TransactionIntegrationTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtUtil jwtUtil;
    @Autowired UserDetailsServiceImpl userDetailsService;
    @Autowired LoginAttemptService loginAttemptService;

    private User owner;
    private User other;
    private Account ownerAccount;
    private Account otherAccount;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        loginAttemptService.resetAll();

        owner = userRepository.save(buildUser("owner@test.com"));
        other = userRepository.save(buildUser("other@test.com"));
        ownerAccount = accountRepository.save(buildAccount(owner, BigDecimal.valueOf(1000)));
        otherAccount = accountRepository.save(buildAccount(other, BigDecimal.valueOf(500)));
        ownerToken = generateToken(owner);
        otherToken = generateToken(other);
    }

    // ── Deposit ───────────────────────────────────────────────────────────────

    @Test
    void deposit_validAmount_updatesBalanceAndReturnsTransaction() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(250));
        request.setDescription("Salary");

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/deposit")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(250))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        BigDecimal newBalance = accountRepository.findById(ownerAccount.getId())
                .orElseThrow().getBalance();
        assertThat(newBalance).isEqualByComparingTo(BigDecimal.valueOf(1250));
    }

    @Test
    void deposit_onOtherUsersAccount_returnsForbidden() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/accounts/" + otherAccount.getId() + "/transactions/deposit")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void deposit_unauthenticated_returnsUnauthorized() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/deposit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // ── Withdraw ──────────────────────────────────────────────────────────────

    @Test
    void withdraw_sufficientBalance_updatesBalanceAndReturnsTransaction() throws Exception {
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(BigDecimal.valueOf(300));
        request.setDescription("ATM");

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/withdraw")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(300))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        BigDecimal newBalance = accountRepository.findById(ownerAccount.getId())
                .orElseThrow().getBalance();
        assertThat(newBalance).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    void withdraw_insufficientBalance_returnsUnprocessableEntity() throws Exception {
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(BigDecimal.valueOf(99999));

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/withdraw")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Insufficient balance"));

        // Balance must be unchanged
        BigDecimal balance = accountRepository.findById(ownerAccount.getId())
                .orElseThrow().getBalance();
        assertThat(balance).isEqualByComparingTo(BigDecimal.valueOf(1000));
    }

    // ── Transfer ──────────────────────────────────────────────────────────────

    @Test
    void transfer_validRequest_updatesBalancesForBothAccounts() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber(otherAccount.getAccountNumber());
        request.setAmount(BigDecimal.valueOf(400));
        request.setDescription("Rent");

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/transfer")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.amount").value(400))
                .andExpect(jsonPath("$.status").value("COMPLETED"));

        BigDecimal senderBalance = accountRepository.findById(ownerAccount.getId())
                .orElseThrow().getBalance();
        BigDecimal receiverBalance = accountRepository.findById(otherAccount.getId())
                .orElseThrow().getBalance();

        assertThat(senderBalance).isEqualByComparingTo(BigDecimal.valueOf(600));
        assertThat(receiverBalance).isEqualByComparingTo(BigDecimal.valueOf(900));
    }

    @Test
    void transfer_insufficientBalance_returnsUnprocessableEntityAndLeavesBalancesUnchanged() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber(otherAccount.getAccountNumber());
        request.setAmount(BigDecimal.valueOf(99999));

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/transfer")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        assertThat(accountRepository.findById(ownerAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(1000));
        assertThat(accountRepository.findById(otherAccount.getId()).orElseThrow().getBalance())
                .isEqualByComparingTo(BigDecimal.valueOf(500));
    }

    @Test
    void transfer_toSameAccount_returnsBadRequest() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber(ownerAccount.getAccountNumber());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/transfer")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void transfer_toNonExistentAccount_returnsNotFound() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("AAAAAAAAAAAAAAAA");
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/transfer")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void transfer_toFrozenAccount_returnsUnprocessableEntity() throws Exception {
        otherAccount.setStatus(Account.AccountStatus.FROZEN);
        accountRepository.save(otherAccount);

        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber(otherAccount.getAccountNumber());
        request.setAmount(BigDecimal.valueOf(100));

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/transfer")
                        .header("Authorization", "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── Transaction history ───────────────────────────────────────────────────

    @Test
    void getHistory_afterDeposit_returnsOneTransaction() throws Exception {
        DepositRequest deposit = new DepositRequest();
        deposit.setAmount(BigDecimal.valueOf(100));
        deposit.setDescription("Test deposit");

        mockMvc.perform(post("/api/accounts/" + ownerAccount.getId() + "/transactions/deposit")
                .header("Authorization", "Bearer " + ownerToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(deposit)));

        mockMvc.perform(get("/api/accounts/" + ownerAccount.getId() + "/transactions")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].description").value("Test deposit"));
    }

    @Test
    void getHistory_emptyAccount_returnsEmptyList() throws Exception {
        mockMvc.perform(get("/api/accounts/" + ownerAccount.getId() + "/transactions")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void getHistory_otherUsersAccount_returnsForbidden() throws Exception {
        mockMvc.perform(get("/api/accounts/" + otherAccount.getId() + "/transactions")
                        .header("Authorization", "Bearer " + ownerToken))
                .andExpect(status().isForbidden());
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
