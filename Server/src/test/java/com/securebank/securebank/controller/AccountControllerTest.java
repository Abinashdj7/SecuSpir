package com.securebank.securebank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.securebank.dto.AccountResponse;
import com.securebank.securebank.dto.CreateAccountRequest;
import com.securebank.securebank.model.Account;
import com.securebank.securebank.security.JwtUtil;
import com.securebank.securebank.security.UserDetailsServiceImpl;
import com.securebank.securebank.service.AccountService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean AccountService accountService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private AccountResponse sampleAccount() {
        return AccountResponse.builder()
                .id(1L)
                .accountNumber("ACC1234567890ABCD")
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1000))
                .status(Account.AccountStatus.ACTIVE)
                .build();
    }

    @Test
    @WithMockUser
    void getMyAccounts_returnsOk() throws Exception {
        when(accountService.getMyAccounts()).thenReturn(List.of(sampleAccount()));

        mockMvc.perform(get("/api/accounts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accountNumber").value("ACC1234567890ABCD"))
                .andExpect(jsonPath("$[0].balance").value(1000));
    }

    @Test
    @WithMockUser
    void getAccountById_returnsOk() throws Exception {
        when(accountService.getAccountById(1L)).thenReturn(sampleAccount());

        mockMvc.perform(get("/api/accounts/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.accountType").value("CHECKING"));
    }

    @Test
    @WithMockUser
    void createAccount_returnsCreated() throws Exception {
        CreateAccountRequest request = new CreateAccountRequest();
        request.setAccountType(Account.AccountType.SAVINGS);

        AccountResponse created = AccountResponse.builder()
                .id(2L)
                .accountNumber("SAV0000000000ABCD")
                .accountType(Account.AccountType.SAVINGS)
                .balance(BigDecimal.ZERO)
                .status(Account.AccountStatus.ACTIVE)
                .build();

        when(accountService.createAccount(any(CreateAccountRequest.class))).thenReturn(created);

        mockMvc.perform(post("/api/accounts")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accountType").value("SAVINGS"))
                .andExpect(jsonPath("$.balance").value(0));
    }

    @Test
    @WithMockUser
    void freezeAccount_returnsOk() throws Exception {
        AccountResponse frozen = AccountResponse.builder()
                .id(1L)
                .accountNumber("ACC1234567890ABCD")
                .accountType(Account.AccountType.CHECKING)
                .balance(BigDecimal.valueOf(1000))
                .status(Account.AccountStatus.FROZEN)
                .build();

        when(accountService.freezeAccount(1L)).thenReturn(frozen);

        mockMvc.perform(patch("/api/accounts/1/freeze").with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FROZEN"));
    }

}
