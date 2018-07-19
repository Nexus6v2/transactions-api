package io.bank.api.transactions.handlers;

import io.bank.api.transactions.dao.RedisDao;
import io.bank.api.transactions.model.Account;
import io.bank.api.transactions.model.dto.AccountDTO;
import io.bank.api.transactions.model.dto.CreateAccountRequest;
import io.bank.api.transactions.utils.Converter;
import io.vertx.rxjava.ext.web.RoutingContext;

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
                .doOnEach(account -> {
                    if (account == null) {
                        context.fail(HTTP_NOT_FOUND);
                    }
                })
                .map(Account::fromHash)
                .map(AccountDTO::fromAccount)
                .map(Converter::convertToJson)
                .subscribe(context.response()::end, context::fail);
    }
    
    public void getAllAccounts(RoutingContext context) {
        redisDao.getKeys(ACCOUNT_KEY_PATTERN)
                .map(redisDao::getHash)
                .map(accountSingle -> accountSingle.toBlocking().value())
                .map(Account::fromHash)
                .map(AccountDTO::fromAccount)
                .toList()
                .subscribe(accounts -> {
                    if (accounts.isEmpty()) {
                        context.fail(HTTP_NOT_FOUND);
                    } else {
                        context.response().end(Converter.convertToJson(accounts));
                    }
                });
    }
    
    public void deleteAccount(RoutingContext context) {
        String accountId = context.pathParam(ACCOUNT_ID);
        if (accountId == null) {
            context.fail(HTTP_BAD_REQUEST);
        }
        redisDao.deleteAccount(getAccountKey(accountId))
                .subscribe(deleted -> {
                    if (deleted) {
                        context.response().setStatusCode(HTTP_NO_CONTENT).end();
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
        CreateAccountRequest createAccountRequest = Converter.convertFromJson(accountRequestBody, CreateAccountRequest.class);
        redisDao.createAccount(Account.fromRequest(createAccountRequest))
                .doOnEach(createdAccount -> {
                    if (createdAccount == null) {
                        context.fail(HTTP_INTERNAL_ERROR);
                    }
                })
                .map(AccountDTO::fromAccount)
                .map(Converter::convertToJson)
                .subscribe(createdAccount -> context.response().end(createdAccount), context::fail);
    }
}
