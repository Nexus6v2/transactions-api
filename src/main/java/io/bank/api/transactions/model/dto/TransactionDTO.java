package io.bank.api.transactions.model.dto;

import io.bank.api.transactions.model.Transaction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDTO {
    private String id;
    private String created;
    private long amount;
    private String currency;
    private String senderId;
    private String recipientId;
    
    public static TransactionDTO fromTransaction(Transaction transaction) {
        return TransactionDTO.builder()
                .id(transaction.getId())
                .created(transaction.getCreated().toString())
                .amount(transaction.getAmount().getNumber().longValueExact())
                .currency(transaction.getAmount().getCurrency().getCurrencyCode())
                .senderId(transaction.getSenderId())
                .recipientId(transaction.getRecipientId())
                .build();
    }
}
