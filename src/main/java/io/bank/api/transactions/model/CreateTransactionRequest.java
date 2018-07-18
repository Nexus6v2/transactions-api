package io.bank.api.transactions.model;

import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class CreateTransactionRequest {
    private long amount;
    private String currencyCode;
    private String senderAccountId;
    private String recipientAccountId;
}
