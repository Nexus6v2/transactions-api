package io.bank.api.transactions.model;

import lombok.Data;

@Data
public class CreateTransactionRequest {
    private long amount;
    private String senderAccountId;
    private String recipientAccountId;
}
