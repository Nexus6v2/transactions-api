package io.bank.api.transactions.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.lambdaworks.redis.RedisURI
import io.bank.api.transactions.dao.RedisDao
import io.bank.api.transactions.model.Account
import io.bank.api.transactions.model.Transaction
import io.bank.api.transactions.model.dto.CreateAccountRequest
import io.bank.api.transactions.model.dto.CreateTransactionRequest
import io.bank.api.transactions.verticles.MainVerticle
import io.vertx.rxjava.core.Vertx
import org.apache.http.client.config.RequestConfig
import org.apache.http.impl.nio.client.CloseableHttpAsyncClient
import org.apache.http.impl.nio.client.HttpAsyncClients
import spock.lang.Shared
import spock.lang.Specification

class BaseIntegrationSpec extends Specification {
    @Shared Vertx vertx
    @Shared RedisDao redisDao
//    @Shared RESTClient restClient
    @Shared CloseableHttpAsyncClient httpClient
    @Shared ObjectMapper objectMapper

    @Shared String LOCALHOST = "localhost"
    @Shared int PORT = 8080
    @Shared int REDIS_PORT = 6379
    @Shared String APPLICATION_JSON = "application/json"
    @Shared String CONTENT_TYPE = "Content-Type"

    protected String TRANSACTIONS_URL = "http://${LOCALHOST}:${PORT}/transactions"
    protected String ACCOUNTS_URL = "http://${LOCALHOST}:${PORT}/accounts"

    def getAccountUrl(accountId) {
        return "http://${LOCALHOST}:${PORT}/accounts/${accountId}"
    }

    def getTransactionUrl(transactionId) {
        return "http://${LOCALHOST}:${PORT}/transactions/${transactionId}"
    }

    def getAccountsTransactionsUrl(accountId) {
        return "http://${LOCALHOST}:${PORT}/accounts/${accountId}/transactions"
    }

    protected CreateAccountRequest createAccountRequest = new CreateAccountRequest()
            .setBalance(10000)
            .setCurrencyCode("USD")
    protected Account testAccountOne = Account.fromRequest(createAccountRequest)
    protected Account testAccountTwo = Account.fromRequest(createAccountRequest)
    protected CreateTransactionRequest createTransactionRequest = new CreateTransactionRequest()
            .setAmount(1000)
            .setCurrencyCode("USD")
            .setSenderAccountId(testAccountOne.getId())
            .setRecipientAccountId(testAccountTwo.getId())
    protected Transaction testTransaction = Transaction.fromRequest(createTransactionRequest)

    // Invoked before first feature method
    def setupSpec() {
        vertx = Vertx.vertx()
        vertx.deployVerticle(MainVerticle.class.getName())

        redisDao = new RedisDao(RedisURI.create(LOCALHOST, REDIS_PORT))

//        restClient = new RESTClient()
//        restClient.setUri("http://${LOCALHOST}:${PORT}")
//        restClient.setContentType(APPLICATION_JSON)
//        restClient.handler.failure = { resp, data ->
//            resp.setData(data)
//            return resp
//        }

        RequestConfig requestConfig = RequestConfig.custom()
                .setSocketTimeout(3000)
                .setConnectTimeout(3000).build()
        httpClient = HttpAsyncClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()
        httpClient.start()

        objectMapper = new ObjectMapper()

        // Waiting for verticles to deploy
        sleep(1000)
    }

    // Invoked after last feature method
    def cleanupSpec() {
//        restClient.shutdown()
        httpClient.close()
        vertx.deploymentIDs().forEach({ id -> vertx.undeploy(id) })
        vertx.close()
    }

    // Invoked after every feature method
    def cleanup() {
        redisDao.flushAll()
    }
}
