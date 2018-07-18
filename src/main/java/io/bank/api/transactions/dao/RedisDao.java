package io.bank.api.transactions.dao;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.support.ConnectionPoolSupport;
import io.bank.api.transactions.model.Account;
import io.bank.api.transactions.model.Transaction;
import lombok.SneakyThrows;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.Map;
import java.util.Objects;

import static io.bank.api.transactions.utils.KeysUtils.getAccountKey;
import static io.bank.api.transactions.utils.KeysUtils.getTransactionKey;
import static java.lang.String.format;

public class RedisDao {
    private static final String ID = "id";
    private static final String BALANCE = "balance";
    private static final String CREATED = "created";
    private static final String CURRENCY = "currency";
    private static final String AMOUNT = "amount";
    private static final String SENDER_ID = "senderId";
    private static final String RECIPIENT_ID = "recipientId";
    
    private final GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;
    
    public RedisDao(RedisURI redisURI) {
        RedisClient redisClient = RedisClient.create(redisURI);
        this.connectionPool = ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, new GenericObjectPoolConfig());
    }
    
    public Observable<String> getKeys(String pattern) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
        return connection.keys(pattern)
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(connection::close);
    }
    
    public Single<Map<String, String>> getHash(String hashKey) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
        return connection.hgetall(hashKey)
                .toSingle()
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(connection::close);
    }
    
    public Single<String> getValue(String valueKey) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
        return connection.get(valueKey)
                .toSingle()
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(connection::close);
    }
    
    public Single<Boolean> deleteValue(String valueKey) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
        // If response is not 0 - value has been deleted
        return connection.del(valueKey)
                .toSingle()
                .subscribeOn(Schedulers.io())
                .map(response -> response != 0)
                .doAfterTerminate(connection::close);
    }
    
    public Single<Boolean> deleteAccount(String hashKey) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
        // If response is not 0 - hash has been deleted
        return connection.hdel(hashKey, ID, CREATED, BALANCE)
                .toSingle()
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(connection::close)
                .map(response -> response != 0);
    }
    
    public Single<Map<String, String>> createAccount(Account account) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
        return connection.multi()
                .toSingle()
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(connection::close)
                .flatMap(multi -> {
                    String accountKey = getAccountKey(account.getId());
                    //Mapping Account fields to Redis Hash
                    connection.hset(accountKey, ID, account.getId()).subscribe();
                    connection.hset(accountKey, BALANCE, String.valueOf(account.getBalance().getNumber().longValueExact())).subscribe();
                    connection.hset(accountKey, CURRENCY, account.getBalance().getCurrency().getCurrencyCode()).subscribe();
                    connection.hset(accountKey, CREATED, String.valueOf(account.getCreated())).subscribe();
                    connection.exec().subscribe();
                    return connection.hgetall(accountKey).toSingle();
                });
    }
    
    public Single<Map> createTransaction(Transaction transaction) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
    
        //Establishing transaction data
        long transactionAmount = transaction.getAmount().getNumber().longValueExact();
        String transactionKey = getTransactionKey(transaction.getId());
        String senderKey = getAccountKey(transaction.getSenderId());
        String recipientKey = getAccountKey(transaction.getRecipientId());
        
        return connection.watch(senderKey, recipientKey)
                .toSingle()
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(connection::unwatch)
                .doAfterTerminate(() ->  {
                    connection.unwatch().subscribe();
                    connection.close();
                })
                .map(watch -> {
//                    //Check if both accounts exist
//                    connection.hexists(senderKey, BALANCE)
//                            .subscribe(exist -> {
//                                if (!exist) {
//                                    throw new IllegalStateException(format("Account with id %s does not exits.", transaction.getSenderId()));
//                                }
//                            });
//                    connection.hexists(recipientKey, BALANCE)
//                            .subscribe(exist -> {
//                                if (!exist) {
//                                    throw new IllegalStateException(format("Account with id %s does not exist.", transaction.getRecipientId()));
//                                }
//                            });
//
//                    //Check if sender balance is enough
//                    connection.hget(senderKey, BALANCE)
//                            .subscribe(balance -> {
//                                if (Long.valueOf(balance) < transactionAmount) {
//                                    throw new IllegalStateException(format("Not enough funds for transaction. Required: %s. Actual: %s.", transactionAmount, balance));
//                                }
//                            });
//
//                    //Check if both accounts and transaction have the same currency
//                    Observable.zip(
//                            connection.hget(senderKey, CURRENCY),
//                            connection.hget(recipientKey, CURRENCY),
//                            (senderAccountCurrency, recipientAccountCurrency) ->
//                                    (Objects.equals(senderAccountCurrency, recipientAccountCurrency) &&
//                                     Objects.equals(senderAccountCurrency, transaction.getAmount().getCurrency().getCurrencyCode()))
//                    ).subscribe(equals -> {
//                        if (!equals) {
//                            throw new IllegalStateException("Recipient, sender and transaction currencies must be the same");
//                        }
//                    });
                    
                    Observable.zip(
                            connection.hexists(senderKey, BALANCE),
                            connection.hexists(recipientKey, BALANCE),
                            connection.hget(senderKey, BALANCE),
                            connection.hget(senderKey, CURRENCY),
                            connection.hget(recipientKey, CURRENCY),
                            (senderAccountExist, recipientAccountExist, senderBalance, senderCurrency, recipientCurrency) -> {
                                if (!senderAccountExist) {
                                    throw new IllegalStateException(format("Account with id %s does not exits.", transaction.getSenderId()));
                                }

                                if (!recipientAccountExist) {
                                    throw new IllegalStateException(format("Account with id %s does not exits.", transaction.getRecipientId()));
                                }

                                if (Long.valueOf(senderBalance) < transactionAmount) {
                                    throw new IllegalStateException(format("Not enough funds for transaction. Required: %s. Actual: %s.", transactionAmount, senderBalance));
                                }

                                if (!(Objects.equals(senderCurrency, recipientCurrency) && Objects.equals(senderCurrency, transaction.getAmount().getCurrency().getCurrencyCode()))) {
                                    throw new IllegalStateException("Recipient, sender and transaction currencies must be the same");
                                }

                                return null;
                            }).subscribe(s -> {}, e -> {
                                throw new IllegalStateException(e.getMessage());
                            });
                    
                    return watch;
                }).flatMap(watch -> {
                    connection.multi().subscribe();
                    //Executing transaction
                    connection.hincrby(senderKey, BALANCE, -transactionAmount).subscribe();
                    connection.hincrby(recipientKey, BALANCE, transactionAmount).subscribe();
                    connection.hset(transactionKey, ID, transaction.getId()).subscribe();
                    connection.hset(transactionKey, AMOUNT, String.valueOf(transaction.getAmount().getNumber().longValueExact())).subscribe();
                    connection.hset(transactionKey, CURRENCY, transaction.getAmount().getCurrency().getCurrencyCode()).subscribe();
                    connection.hset(transactionKey, CREATED, String.valueOf(transaction.getCreated())).subscribe();
                    connection.hset(transactionKey, SENDER_ID, transaction.getSenderId()).subscribe();
                    connection.hset(transactionKey, RECIPIENT_ID, transaction.getRecipientId()).subscribe();
                    connection.exec().subscribe();
                    
                    return connection.hgetall(transactionKey).toSingle();
                });
    }
    
    public void flushAll() {
        StatefulRedisConnection<String, String> connection = borrowConnection();
        connection.sync().flushall();
        connection.close();
    }
    
    @SneakyThrows
    private StatefulRedisConnection<String, String> borrowConnection() {
        return connectionPool.borrowObject();
    }
}
