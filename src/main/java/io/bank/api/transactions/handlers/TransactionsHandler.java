package io.bank.api.transactions.handlers;

import io.bank.api.transactions.dao.RedisDao;
import io.bank.api.transactions.model.CreateTransactionRequest;
import io.bank.api.transactions.model.Transaction;
import io.bank.api.transactions.utils.Converter;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.Objects;

import static io.bank.api.transactions.handlers.AccountsHandler.ACCOUNT_ID;
import static io.bank.api.transactions.utils.KeysUtils.TRANSACTION_KEY_PATTERN;
import static io.bank.api.transactions.utils.KeysUtils.getTransactionKey;
import static java.net.HttpURLConnection.*;

public class TransactionsHandler {
    private static final String TRANSACTION_ID = "transactionId";
    
    private final RedisDao redisDao;
    
    public TransactionsHandler(RedisDao redisDao) {
        this.redisDao = redisDao;
    }
    
    public void getTransaction(RoutingContext context) {
        String transactionId = context.pathParam(TRANSACTION_ID);
        if (transactionId == null) {
            context.response().setStatusCode(HTTP_BAD_REQUEST).end();
        }
        redisDao.getValue(getTransactionKey(transactionId))
                .subscribe(context.response()::end, context::fail);
    }
    
    public void getAllTransactions(RoutingContext context) {
        redisDao.getKeys(TRANSACTION_KEY_PATTERN)
                .map(redisDao::getValue)
                .map(transactionSingle -> transactionSingle.toBlocking().value())
                .toList()
                .subscribe(transactions -> {
                    if (transactions.isEmpty()) {
                        context.fail(HTTP_NOT_FOUND);
                    } else {
                        context.response().end(Converter.toJson(transactions));
                    }
                }, context::fail);
    }
    
    public void getAccountsTransactions(RoutingContext context) {
        String accountId = context.pathParam(ACCOUNT_ID);
        if (accountId == null) {
            context.fail(HTTP_BAD_REQUEST);
        }
        
        redisDao.getKeys(TRANSACTION_KEY_PATTERN)
                .map(redisDao::getValue)
                .map(transactionSingle -> transactionSingle.toBlocking().value())
                .map(transactionJson -> Converter.fromJson(transactionJson, Transaction.class))
                .filter(transaction -> Objects.equals(accountId, transaction.getSenderId()) ||
                                       Objects.equals(accountId, transaction.getRecipientId()))
                .toList()
                .subscribe(transactions -> {
                    if (transactions.isEmpty()) {
                        context.fail(HTTP_NOT_FOUND);
                    } else {
                        context.response().end(Converter.toJson(transactions));
                    }
                }, context::fail);
    }
    
    public void createTransaction(RoutingContext context) {
        String transactionRequestBody = context.getBodyAsString();
        if (transactionRequestBody == null) {
            context.fail(HTTP_BAD_REQUEST);
        }
        CreateTransactionRequest createTransactionRequest = Converter.fromJson(transactionRequestBody, CreateTransactionRequest.class);
        Transaction transaction = Transaction.fromRequest(createTransactionRequest);
        redisDao.createTransaction(transaction)
                .subscribe(executedTransaction -> {
                    if (executedTransaction != null) {
                        context.response().end(Converter.toJson(executedTransaction));
                    } else {
                        context.fail(HTTP_INTERNAL_ERROR);
                    }
                }, context::fail);
    }
}
