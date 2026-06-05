package com.securebank.securebank.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.securebank.dto.LoginRequest;
import com.securebank.securebank.dto.RegisterRequest;
import com.securebank.securebank.model.User;
import com.securebank.securebank.repo.AccountRepository;
import com.securebank.securebank.repo.TransactionRepository;
import com.securebank.securebank.repo.UserRepository;
import com.securebank.securebank.security.LoginAttemptService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("integration")
class AuthIntegrationTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TransactionRepository transactionRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired LoginAttemptService loginAttemptService;

    @BeforeEach
    void setUp() {
        transactionRepository.deleteAll();
        accountRepository.deleteAll();
        userRepository.deleteAll();
        loginAttemptService.resetAll();
    }

    // ── Register ──────────────────────────────────────────────────────────────

    @Test
    void register_validRequest_createsUserAccountAndReturnsToken() throws Exception {
        RegisterRequest request = buildRegisterRequest("john@test.com");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("john@test.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));

        assertThat(userRepository.findByEmail("john@test.com")).isPresent();
        assertThat(accountRepository.findAll()).hasSize(1);
    }

    @Test
    void register_duplicateEmail_returnsConflict() throws Exception {
        userRepository.save(existingUser("jane@test.com"));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRegisterRequest("jane@test.com"))))
                .andExpect(status().isConflict());

        assertThat(userRepository.findAll()).hasSize(1);
    }

    @Test
    void register_invalidEmail_returnsBadRequest() throws Exception {
        RegisterRequest request = buildRegisterRequest("not-an-email");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.email").exists());
    }

    @Test
    void register_weakPassword_returnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFirstName("John");
        request.setLastName("Doe");
        request.setEmail("john@test.com");
        request.setPassword("weakpassword"); // no uppercase, digit, or special char

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.password").exists());
    }

    @Test
    void register_missingFirstName_returnsBadRequest() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setLastName("Doe");
        request.setEmail("john@test.com");
        request.setPassword("Test1234!");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Test
    void login_validCredentials_returnsTokenAndEmail() throws Exception {
        userRepository.save(existingUser("alice@test.com"));

        LoginRequest request = new LoginRequest();
        request.setEmail("alice@test.com");
        request.setPassword("Test1234!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.email").value("alice@test.com"))
                .andExpect(jsonPath("$.role").value("ROLE_USER"));
    }

    @Test
    void login_invalidPassword_returnsUnauthorized() throws Exception {
        userRepository.save(existingUser("bob@test.com"));

        LoginRequest request = new LoginRequest();
        request.setEmail("bob@test.com");
        request.setPassword("WrongPass1!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_nonExistentEmail_returnsUnauthorized() throws Exception {
        LoginRequest request = new LoginRequest();
        request.setEmail("nobody@test.com");
        request.setPassword("Test1234!");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_rateLimiting_blocksAfterFiveFailedAttempts() throws Exception {
        userRepository.save(existingUser("rate@test.com"));

        LoginRequest badRequest = new LoginRequest();
        badRequest.setEmail("rate@test.com");
        badRequest.setPassword("WrongPass1!");

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(badRequest)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th attempt — same IP is now blocked
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badRequest)))
                .andExpect(status().isTooManyRequests());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private RegisterRequest buildRegisterRequest(String email) {
        RegisterRequest r = new RegisterRequest();
        r.setFirstName("John");
        r.setLastName("Doe");
        r.setEmail(email);
        r.setPassword("Test1234!");
        return r;
    }

    private User existingUser(String email) {
        return User.builder()
                .firstName("Test").lastName("User")
                .email(email)
                .password(passwordEncoder.encode("Test1234!"))
                .role(User.Role.ROLE_USER)
                .build();
    }
}
