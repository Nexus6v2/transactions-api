package io.bank.api.transactions.model;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

import static io.bank.api.transactions.utils.CommonUtils.getShortId;

@Data
@Builder
public class Account {
    private String id;
    private String created;
    private long balance;
    
    public static Account fromRequest(CreateAccountRequest request) {
        return Account.builder()
                .id(getShortId())
                .created(LocalDateTime.now().toString())
                .balance(request.getInitialBalance())
                .build();
    }
}
