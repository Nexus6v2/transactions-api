package io.bank.api.transactions.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import static io.bank.api.transactions.utils.CommonUtils.getShortId;

@Data
@Builder
public class Transaction {
    private String id;
    private String created;
    private long amount;
    private String senderId;
    private String recipientId;
    
    public static Transaction fromRequest(CreateTransactionRequest request) {
        return Transaction.builder()
                .id(getShortId())
                .created(LocalDateTime.now().toString())
                .amount(request.getTransactionAmount())
                .senderId(request.getSenderAccountId())
                .recipientId(request.getRecipientAccountId())
                .build();
    }
}
