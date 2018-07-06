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
public class Account {
    private String id;
    private String created;
    private String balance;
    
    public static Account fromRequest(CreateAccountRequest request) {
        return Account.builder()
                .id(getShortId())
                .created(LocalDateTime.now().toString())
                .balance(request.getBalance())
                .build();
    }
}
