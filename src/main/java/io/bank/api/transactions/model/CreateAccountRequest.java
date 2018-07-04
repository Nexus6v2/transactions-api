package io.bank.api.transactions.model;

import lombok.Data;

@Data
public class CreateAccountRequest {
    private long initialBalance;
}
