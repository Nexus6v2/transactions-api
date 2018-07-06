package io.bank.api.transactions.handlers;

import io.bank.api.transactions.dao.RedisDao;
import io.bank.api.transactions.model.Account;
import io.bank.api.transactions.model.CreateAccountRequest;
import io.bank.api.transactions.utils.Converter;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.bank.api.transactions.utils.KeysUtils.ACCOUNT_KEY_PATTERN;
import static io.bank.api.transactions.utils.KeysUtils.getAccountKey;
import static java.net.HttpURLConnection.*;

public class AccountsHandler {
    static final String ACCOUNT_ID = "accountId";
    
    private final RedisDao redisDao;
    
    public AccountsHandler(RedisDao redisDao) {
        this.redisDao = redisDao;
    }
    
    public void getAccount(RoutingContext context) {
        String accountId = context.pathParam(ACCOUNT_ID);
        if (accountId == null) {
            context.fail(HTTP_BAD_REQUEST);
        }
        redisDao.getHash(getAccountKey(accountId))
                .map(account -> {
                    if (account.isEmpty()) {
                        context.fail(HTTP_NOT_FOUND);
                        return "";
                    } else {
                        return account;
                    }
                })
                .map(Converter::toJson)
                .subscribe(context.response()::end, context::fail);
    }
    
    public void getAllAccounts(RoutingContext context) {
        List<Map> accounts = redisDao.getKeys(ACCOUNT_KEY_PATTERN)
                .parallelStream()
                .map(redisDao::getHash)
                .map(accountSingle -> accountSingle.toBlocking().value())
                .collect(Collectors.toList());
        
        if (accounts.isEmpty()) {
            context.fail(HTTP_NOT_FOUND);
        } else {
            context.response().end(Converter.toJson(accounts));
        }
    }
    
    public void deleteAccount(RoutingContext context) {
        String accountId = context.pathParam(ACCOUNT_ID);
        if (accountId == null) {
            context.fail(HTTP_BAD_REQUEST);
        }
        redisDao.deleteAccount(getAccountKey(accountId))
                .subscribe(deleted -> {
                    if (deleted) {
                        context.response().setStatusCode(HTTP_OK).end();
                    } else {
                        context.fail(HTTP_NOT_FOUND);
                    }
                }, context::fail);
    }
    
    public void createAccount(RoutingContext context) {
        String accountRequestBody = context.getBodyAsString();
        if (accountRequestBody == null) {
            context.response().setStatusCode(HTTP_BAD_REQUEST).end();
        }
        CreateAccountRequest createAccountRequest = Converter.fromJson(accountRequestBody, CreateAccountRequest.class);
        Account account = Account.fromRequest(createAccountRequest);
        redisDao.createAccount(account)
                .subscribe(createdAccount -> {
                    if (createdAccount != null) {
                        context.response().setStatusCode(HTTP_OK).end(Converter.toJson(createdAccount));
                    } else {
                        context.fail(HTTP_INTERNAL_ERROR);
                    }
                }, context::fail);
    }
}
