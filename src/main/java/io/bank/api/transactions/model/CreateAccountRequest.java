package io.bank.api.transactions.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateAccountRequest {
    private long balance;
    private String currencyCode;
}
