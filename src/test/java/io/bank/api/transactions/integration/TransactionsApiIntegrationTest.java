package io.bank.api.transactions.integration;

import com.lambdaworks.redis.RedisURI;
import io.bank.api.transactions.dao.RedisDao;
import io.bank.api.transactions.model.Account;
import io.bank.api.transactions.model.CreateAccountRequest;
import io.bank.api.transactions.model.CreateTransactionRequest;
import io.bank.api.transactions.verticles.MainVerticle;
import io.vertx.core.json.Json;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.rxjava.core.Vertx;
import lombok.SneakyThrows;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static java.lang.String.format;

@RunWith(VertxUnitRunner.class)
public class TransactionsApiIntegrationTest {
    // Common test config
    private static final String LOCALHOST = "localhost";
    private static final int PORT = 8080;
    
    // Endpoints
    private static final String HEALTHCHECK_URL = "/health";
    private static final String ACCOUNTS_URL = "/accounts";
    private static final String TRANSACTIONS_URL = "/transactions";
    private static String getAccountUrl(String accountId) {
        return format("%s/%s", ACCOUNTS_URL, accountId);
    }
    private static String getTransactionUrl(String transactionId) {
        return format("%s/%s", TRANSACTIONS_URL, transactionId);
    }
    private static String getAccountsTransactionsUrl(String accountId) {
        return format("%s/%s%s", ACCOUNTS_URL, accountId, TRANSACTIONS_URL);
    }
    
    // Test data setup
    private static final CreateAccountRequest createAccountRequest = new CreateAccountRequest().setBalance(100000);
    private static final Account testAccountOne = Account.fromRequest(createAccountRequest);
    private static final Account testAccountTwo = Account.fromRequest(createAccountRequest);
    private static final CreateTransactionRequest createTransactionRequest = new CreateTransactionRequest()
            .setAmount(1000)
            .setRecipientAccountId(testAccountOne.getId())
            .setSenderAccountId(testAccountTwo.getId());
    
    // Redis test setup
    private static final RedisURI redisURI = RedisURI.builder()
            .withHost("localhost")
            .withPort(6379)
            .build();
    private static final RedisDao redisDao = new RedisDao(redisURI);
    
    // Vertx
    private Vertx vertx;
    
    @Before
    @SneakyThrows
    public void testSetup(TestContext context) {
        vertx = Vertx.vertx();
        vertx.deployVerticle(MainVerticle.class.getName(), context.asyncAssertSuccess());
        
        // Creating accounts in database
        redisDao.createAccount(testAccountOne);
        redisDao.createAccount(testAccountTwo);
    }
    
    @After
    public void tearDown(TestContext context) {
        vertx.close(context.asyncAssertSuccess());
    }
    
    
    @Test
    public void checkHealth(TestContext context) {
        final Async test = context.async();
        vertx.createHttpClient().get(PORT, LOCALHOST, HEALTHCHECK_URL)
                .handler(response -> response.bodyHandler(body -> {
                    context.assertTrue(body.toString().contains("UP"));
                    test.complete();
                })).end();
    }
    
    @Test
    public void createAccount(TestContext context) {
        final Async test = context.async();
        vertx.createHttpClient().post(PORT, LOCALHOST, getAccountUrl(testAccountOne.getId()))
                .putHeader("content-type", "application/json")
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 200);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                        Account account = Json.decodeValue(body.toString(), Account.class);
                        context.assertEquals(account.getId(), testAccountOne.getId());
                        context.assertEquals(account.getBalance(), testAccountOne.getBalance());
                        context.assertEquals(account.getCreated(), testAccountOne.getCreated());
                        test.complete();
                    });
                })
                .end();
    }
    
    @Test
    public void deleteAccount(TestContext context) {
        final Async test = context.async();
        
        vertx.createHttpClient().delete(LOCALHOST, ACCOUNTS_URL, response -> {});
    }
    
    @Test
    public void getTransaction(TestContext context) {
        final Async test = context.async();
        
        vertx.createHttpClient().post(LOCALHOST, TRANSACTIONS_URL, response -> {});
    }
    
    @Test
    public void createTransaction(TestContext context) {
        final Async test = context.async();
        
        vertx.createHttpClient().post(LOCALHOST, TRANSACTIONS_URL, response -> {});
    }
    
}

