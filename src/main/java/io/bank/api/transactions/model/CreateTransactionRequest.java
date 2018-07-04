package io.bank.api.transactions.model;

import lombok.Data;

@Data
public class CreateTransactionRequest {
    private long transactionAmount;
    private String senderAccountId;
    private String recipientAccountId;
}
