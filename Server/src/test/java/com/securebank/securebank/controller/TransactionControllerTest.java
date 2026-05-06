package com.securebank.securebank.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.securebank.securebank.dto.DepositRequest;
import com.securebank.securebank.dto.TransactionResponse;
import com.securebank.securebank.dto.TransferRequest;
import com.securebank.securebank.dto.WithdrawRequest;
import com.securebank.securebank.model.Transaction;
import com.securebank.securebank.security.JwtUtil;
import com.securebank.securebank.security.UserDetailsServiceImpl;
import com.securebank.securebank.service.TransactionService;
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

@WebMvcTest(TransactionController.class)
class TransactionControllerTest {

    @Autowired MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @MockitoBean TransactionService transactionService;
    @MockitoBean JwtUtil jwtUtil;
    @MockitoBean UserDetailsServiceImpl userDetailsService;

    private TransactionResponse depositResponse() {
        return TransactionResponse.builder()
                .id(1L)
                .receiverAccountNumber("ACC1234567890ABCD")
                .amount(BigDecimal.valueOf(500))
                .type(Transaction.TransactionType.DEPOSIT)
                .status(Transaction.TransactionStatus.COMPLETED)
                .description("Salary")
                .build();
    }

    @Test
    @WithMockUser
    void deposit_returnsOk() throws Exception {
        DepositRequest request = new DepositRequest();
        request.setAmount(BigDecimal.valueOf(500));
        request.setDescription("Salary");

        when(transactionService.deposit(eq(1L), any(DepositRequest.class))).thenReturn(depositResponse());

        mockMvc.perform(post("/api/accounts/1/transactions/deposit")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("DEPOSIT"))
                .andExpect(jsonPath("$.amount").value(500))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    @WithMockUser
    void withdraw_returnsOk() throws Exception {
        WithdrawRequest request = new WithdrawRequest();
        request.setAmount(BigDecimal.valueOf(200));
        request.setDescription("ATM");

        TransactionResponse response = TransactionResponse.builder()
                .id(2L)
                .senderAccountNumber("ACC1234567890ABCD")
                .amount(BigDecimal.valueOf(200))
                .type(Transaction.TransactionType.WITHDRAWAL)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        when(transactionService.withdraw(eq(1L), any(WithdrawRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts/1/transactions/withdraw")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("WITHDRAWAL"))
                .andExpect(jsonPath("$.amount").value(200));
    }

    @Test
    @WithMockUser
    void transfer_returnsOk() throws Exception {
        TransferRequest request = new TransferRequest();
        request.setReceiverAccountNumber("RECV0000000000AB");
        request.setAmount(BigDecimal.valueOf(300));
        request.setDescription("Rent");

        TransactionResponse response = TransactionResponse.builder()
                .id(3L)
                .senderAccountNumber("ACC1234567890ABCD")
                .receiverAccountNumber("RECV0000000000AB")
                .amount(BigDecimal.valueOf(300))
                .type(Transaction.TransactionType.TRANSFER)
                .status(Transaction.TransactionStatus.COMPLETED)
                .build();

        when(transactionService.transfer(eq(1L), any(TransferRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/accounts/1/transactions/transfer")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("TRANSFER"))
                .andExpect(jsonPath("$.senderAccountNumber").value("ACC1234567890ABCD"))
                .andExpect(jsonPath("$.receiverAccountNumber").value("RECV0000000000AB"));
    }

    @Test
    @WithMockUser
    void getHistory_returnsOk() throws Exception {
        when(transactionService.getHistory(1L)).thenReturn(List.of(depositResponse()));

        mockMvc.perform(get("/api/accounts/1/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].type").value("DEPOSIT"))
                .andExpect(jsonPath("$[0].amount").value(500));
    }

}
