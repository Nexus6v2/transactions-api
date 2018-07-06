package io.bank.api.transactions.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static io.bank.api.transactions.utils.CommonUtils.getShortId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private String created;
    private String amount;
    private String senderId;
    private String recipientId;
    
    public static Transaction fromRequest(CreateTransactionRequest request) {
        return Transaction.builder()
                .id(getShortId())
                .created(LocalDateTime.now().toString())
                .amount(request.getAmount())
                .senderId(request.getSenderAccountId())
                .recipientId(request.getRecipientAccountId())
                .build();
    }
}
