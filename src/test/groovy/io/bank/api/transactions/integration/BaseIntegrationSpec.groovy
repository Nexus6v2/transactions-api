package io.bank.api.transactions.integration

import com.fasterxml.jackson.databind.ObjectMapper
import com.lambdaworks.redis.RedisURI
import groovyx.net.http.RESTClient
import io.bank.api.transactions.dao.RedisDao
import io.bank.api.transactions.model.Account
import io.bank.api.transactions.model.CreateAccountRequest
import io.bank.api.transactions.model.CreateTransactionRequest
import io.bank.api.transactions.model.Transaction
import io.bank.api.transactions.verticles.MainVerticle
import io.vertx.rxjava.core.Vertx
import spock.lang.Shared
import spock.lang.Specification

class BaseIntegrationSpec extends Specification {
    @Shared Vertx vertx
    @Shared RedisDao redisDao
    @Shared RESTClient restClient
    @Shared ObjectMapper objectMapper

    @Shared String LOCALHOST = "localhost"
    @Shared int PORT = 8080
    @Shared int REDIS_PORT = 6379
    @Shared String APPLICATION_JSON = "application/json"

    protected String TRANSACTIONS_URL = "/transactions"
    protected String ACCOUNTS_URL = "/accounts"

    protected CreateAccountRequest createAccountRequest = new CreateAccountRequest().setBalance(100000)
    protected Account testAccountOne = Account.fromRequest(createAccountRequest)
    protected Account testAccountTwo = Account.fromRequest(createAccountRequest)
    protected CreateTransactionRequest createTransactionRequest = new CreateTransactionRequest()
            .setAmount(100)
            .setSenderAccountId(testAccountOne.getId())
            .setRecipientAccountId(testAccountTwo.getId())
    protected Transaction testTransaction = Transaction.fromRequest(createTransactionRequest)

    def accountUrl(accountId) {
        return "${ACCOUNTS_URL}/${accountId}"
    }

    def transactionUrl(transactionId) {
        return "${TRANSACTIONS_URL}/${transactionId}"
    }

    def accountsTransactionsUrl(accountId) {
        return "${ACCOUNTS_URL}/${accountId}/${TRANSACTIONS_URL}"
    }

    // Invoked before first feature method
    def setupSpec() {
        vertx = Vertx.vertx()
        vertx.deployVerticle(MainVerticle.class.getName())

        redisDao = new RedisDao(RedisURI.create(LOCALHOST, REDIS_PORT))

        restClient = new RESTClient()
        restClient.setUri("http://${LOCALHOST}:${PORT}")
        restClient.setContentType(APPLICATION_JSON)
        restClient.handler.failure = { resp, data ->
            resp.setData(data)
            return resp
        }

        objectMapper = new ObjectMapper()

        // Waiting for verticles to deploy
        sleep(1000)
    }

    // Invoked after last feature method
    def cleanupSpec() {
        vertx.deploymentIDs().forEach({ id -> vertx.undeploy(id) })
        vertx.close()
    }

    // Invoked after every feature method
    def cleanup() {
        redisDao.flushAll()
    }
}
