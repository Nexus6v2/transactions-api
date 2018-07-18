package io.bank.api.transactions.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.javamoney.moneta.Money;

import javax.money.Monetary;
import java.time.LocalDateTime;

import static io.bank.api.transactions.utils.CommonUtils.getShortId;

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
}
