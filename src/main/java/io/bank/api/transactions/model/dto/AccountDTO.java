package io.bank.api.transactions.model.dto;

import io.bank.api.transactions.model.Account;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccountDTO {
    private String id;
    private String created;
    private String currency;
    private long balance;
    
    public static AccountDTO fromAccount(Account account) {
        return AccountDTO.builder()
                .id(account.getId())
                .created(account.getCreated().toString())
                .currency(account.getBalance().getCurrency().getCurrencyCode())
                .balance(account.getBalance().getNumber().longValueExact())
                .build();
    }
}
