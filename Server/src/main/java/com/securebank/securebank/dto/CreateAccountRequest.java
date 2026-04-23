package com.securebank.securebank.dto;

import com.securebank.securebank.model.Account;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateAccountRequest {

    @NotNull(message = "Account type is required")
    private Account.AccountType accountType; // CHECKING or SAVINGS
}