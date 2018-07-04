package io.bank.api.transactions.handlers;

import io.bank.api.transactions.dao.RedisDao;
import io.bank.api.transactions.model.CreateTransactionRequest;
import io.bank.api.transactions.model.Transaction;
import io.bank.api.transactions.utils.Converter;
import io.vertx.rxjava.ext.web.RoutingContext;

import java.util.List;
import java.util.stream.Collectors;

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
        List<String> transactions = redisDao.getKeys(TRANSACTION_KEY_PATTERN)
                .parallelStream()
                .map(redisDao::getValue)
                .map(transactionSingle -> transactionSingle.toBlocking().value())
                .collect(Collectors.toList());
        
        if (transactions.isEmpty()) {
            context.fail(HTTP_NOT_FOUND);
        } else {
            context.response().end(Converter.toJson(transactions));
        }
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
                        context.response().end(executedTransaction);
                    } else {
                        context.fail(HTTP_INTERNAL_ERROR);
                    }
                }, context::fail);
    }
}
