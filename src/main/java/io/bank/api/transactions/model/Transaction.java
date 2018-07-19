package io.bank.api.transactions.model;

import io.bank.api.transactions.model.dto.CreateTransactionRequest;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javamoney.moneta.Money;

import javax.money.Monetary;
import java.time.LocalDateTime;
import java.util.Map;

import static io.bank.api.transactions.utils.CommonUtils.getShortId;
import static io.bank.api.transactions.utils.KeysUtils.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Transaction {
    private String id;
    private LocalDateTime created;
    private Money amount;
    private String senderId;
    private String recipientId;
    
    public static Transaction fromRequest(CreateTransactionRequest request) {
        return Transaction.builder()
                .id(getShortId())
                .created(LocalDateTime.now())
                .amount(Money.of(request.getAmount(), Monetary.getCurrency(request.getCurrencyCode())))
                .senderId(request.getSenderAccountId())
                .recipientId(request.getRecipientAccountId())
                .build();
    }
    
    public static Transaction fromHash(Map<String, String> hash) {
        return Transaction.builder()
                .id(hash.get(ID))
                .created(LocalDateTime.parse(hash.get(CREATED)))
                .amount(Money.of(Long.valueOf(hash.get(AMOUNT)), Monetary.getCurrency(hash.get(CURRENCY))))
                .senderId(hash.get(SENDER_ID))
                .recipientId(hash.get(RECIPIENT_ID))
                .build();
    }
}
