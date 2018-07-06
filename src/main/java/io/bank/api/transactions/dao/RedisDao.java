package io.bank.api.transactions.dao;

import com.lambdaworks.redis.RedisClient;
import com.lambdaworks.redis.RedisURI;
import com.lambdaworks.redis.api.StatefulRedisConnection;
import com.lambdaworks.redis.api.rx.RedisReactiveCommands;
import com.lambdaworks.redis.api.sync.RedisCommands;
import com.lambdaworks.redis.support.ConnectionPoolSupport;
import io.bank.api.transactions.model.Account;
import io.bank.api.transactions.model.Transaction;
import io.bank.api.transactions.utils.Converter;
import lombok.SneakyThrows;
import org.apache.commons.pool2.impl.GenericObjectPool;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import rx.Observable;
import rx.Single;
import rx.schedulers.Schedulers;

import java.util.List;
import java.util.Map;

import static io.bank.api.transactions.utils.KeysUtils.getAccountKey;
import static io.bank.api.transactions.utils.KeysUtils.getTransactionKey;

public class RedisDao {
    private static final String ACCOUNT_ID = "id";
    private static final String ACCOUNT_BALANCE = "balance";
    private static final String ACCOUNT_CREATED = "created";
    
    private final GenericObjectPool<StatefulRedisConnection<String, String>> connectionPool;
    
    public RedisDao(RedisURI redisURI) {
        RedisClient redisClient = RedisClient.create(redisURI);
        this.connectionPool = ConnectionPoolSupport.createGenericObjectPool(redisClient::connect, new GenericObjectPoolConfig());
    }
    
    public List<String> getKeys(String pattern) {
        RedisCommands<String, String> connection = borrowConnection().sync();
        List<String> keys = connection.keys(pattern);
        connection.close();
        return keys;
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
        return connection.hdel(hashKey, ACCOUNT_ID, ACCOUNT_CREATED, ACCOUNT_BALANCE)
                .toSingle()
                .subscribeOn(Schedulers.io())
                .map(response -> response != 0)
                .doAfterTerminate(connection::close);
    }
    
    public Single<Map<String, String>> createAccount(Account account) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
        return connection.multi()
                .single()
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(connection::close)
                .map(multi -> {
                    String accountKey = getAccountKey(account.getId());
                    //Mapping Account fields to Redis Hash
                    connection.hset(accountKey, ACCOUNT_ID, account.getId()).subscribe();
                    connection.hset(accountKey, ACCOUNT_BALANCE, String.valueOf(account.getBalance())).subscribe();
                    connection.hset(accountKey, ACCOUNT_CREATED, account.getCreated()).subscribe();
                    //Commiting
                    connection.exec().subscribe();
                    
                    return connection.hgetall(accountKey).toSingle();
                }).toBlocking().single();
    }
    
    public Single<String> createTransaction(Transaction transaction) {
        RedisReactiveCommands<String, String> connection = borrowConnection().reactive();
    
        //Establishing transaction data
        long transactionAmount = Long.valueOf(transaction.getAmount());
        String senderKey = getAccountKey(transaction.getSenderId());
        String recipientKey = getAccountKey(transaction.getRecipientId());
        
        //Check if both accounts exist
        if (Observable.zip(
                connection.hexists(senderKey, ACCOUNT_BALANCE),
                connection.hexists(recipientKey, ACCOUNT_BALANCE),
                (senderExist, recipientExist) -> !(senderExist && recipientExist)
        ).toBlocking().single()) {
            return Single.error(new IllegalArgumentException("Invalid accounts"));
        }
    
        //Check if sender balance is enough
        if (connection.hget(senderKey, ACCOUNT_BALANCE)
                .map((balance) -> Long.valueOf(balance) < transactionAmount)
                .toBlocking().single()) {
            return Single.error(new IllegalStateException("Not enough funds for transaction"));
        }
        
        return connection.multi()
                .single()
                .subscribeOn(Schedulers.io())
                .doAfterTerminate(connection::close)
                .map(multi -> {
                    //Executing transaction
                    connection.hincrby(senderKey, ACCOUNT_BALANCE, -transactionAmount).subscribe();
                    connection.hincrby(recipientKey, ACCOUNT_BALANCE, transactionAmount).subscribe();
    
                    //Saving transaction info
                    String transactionKey = getTransactionKey(transaction.getId());
                    connection.set(transactionKey, Converter.toJson(transaction)).subscribe();
    
                    //Committing
                    connection.exec().subscribe();
                    
                    return connection.get(transactionKey).toSingle();
                }).toBlocking().single();
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
