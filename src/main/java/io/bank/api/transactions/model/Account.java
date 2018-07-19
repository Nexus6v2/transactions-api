package io.bank.api.transactions.model;

import io.bank.api.transactions.model.dto.CreateAccountRequest;
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
public class Account {
    private String id;
    private LocalDateTime created;
    private Money balance;
    
    public static Account fromRequest(CreateAccountRequest request) {
        return Account.builder()
                .id(getShortId())
                .created(LocalDateTime.now())
                .balance(Money.of(request.getBalance(), Monetary.getCurrency(request.getCurrencyCode())))
                .build();
    }
    
    public static Account fromHash(Map<String, String> hash) {
        return Account.builder()
                .id(hash.get(ID))
                .created(LocalDateTime.parse(hash.get(CREATED)))
                .balance(Money.of(Long.valueOf(hash.get(BALANCE)), Monetary.getCurrency(hash.get(CURRENCY))))
                .build();
    }
}
