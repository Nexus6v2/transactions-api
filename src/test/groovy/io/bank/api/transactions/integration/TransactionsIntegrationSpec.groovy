package io.bank.api.transactions.integration

import io.bank.api.transactions.model.Account
import io.bank.api.transactions.model.CreateTransactionRequest
import io.bank.api.transactions.model.Transaction
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class TransactionsIntegrationSpec extends BaseIntegrationSpec {

    def "Return 404 if requested account does not exist"() {
        when:
        def response = restClient.get(path: accountUrl("someInvalidId"))

        then:
        assert response.status == 404
    }

    def "Return 500 if sender account does not exist"() {
        setup:
        redisDao.createAccount(testAccountOne)
        redisDao.createAccount(testAccountTwo)
        CreateTransactionRequest transactionRequest = new CreateTransactionRequest()
                .setAmount(10000000000)
                .setSenderAccountId("someInvalidId")
                .setRecipientAccountId(testAccountTwo.id)

        when:
        def response = restClient.post(
                path: TRANSACTIONS_URL,
                requestContentType: APPLICATION_JSON,
                body: objectMapper.writeValueAsString(transactionRequest)
        )

        then:
        assert response.status == 500
    }

    def "Return 500 if sender balance is not enough"() {
        setup:
        redisDao.createAccount(testAccountOne)
        redisDao.createAccount(testAccountTwo)
        CreateTransactionRequest transactionRequest = new CreateTransactionRequest()
                .setAmount(10000000000)
                .setSenderAccountId(testAccountOne.id)
                .setRecipientAccountId(testAccountTwo.id)

        when:
        def response = restClient.post(
                path: TRANSACTIONS_URL,
                requestContentType: APPLICATION_JSON,
                body: objectMapper.writeValueAsString(transactionRequest)
        )

        then:
        assert response.status == 500
    }

    def "Get all accounts"() {
        setup:
        def testAccounts = [Account.fromRequest(createAccountRequest), Account.fromRequest(createAccountRequest), Account.fromRequest(createAccountRequest)]
        testAccounts.forEach({ account -> redisDao.createAccount(account) })

        when:
        def response = restClient.get(path: ACCOUNTS_URL)

        then:
        JSONAssert.assertEquals(objectMapper.writeValueAsString(testAccounts), objectMapper.writeValueAsString(response.data), JSONCompareMode.LENIENT)
    }

    def "Get account by id"() {
        setup:
        redisDao.createAccount(testAccountOne)

        when:
        def response = restClient.get(path: accountUrl(testAccountOne.id))

        then:
        JSONAssert.assertEquals(objectMapper.writeValueAsString(testAccountOne), objectMapper.writeValueAsString(response.data), JSONCompareMode.LENIENT)
    }

    def "Create account"() {
        when:
        def response = restClient.post(
                path: ACCOUNTS_URL,
                requestContentType: APPLICATION_JSON,
                body: objectMapper.writeValueAsString(createAccountRequest)
        )

        then:
        assert createAccountRequest.balance == response.data.balance
    }

    def "Delete account"() {
        setup:
        redisDao.createAccount(testAccountOne)

        when:
        def response = restClient.delete(path: accountUrl(testAccountOne.id))

        then:
        response.status == 200
    }

    def "Get all transactions"() {
        setup:
        redisDao.createAccount(testAccountOne)
        redisDao.createAccount(testAccountTwo)
        def testTransactions = [Transaction.fromRequest(createTransactionRequest), Transaction.fromRequest(createTransactionRequest), Transaction.fromRequest(createTransactionRequest)]
        testTransactions.forEach({ transaction -> redisDao.createTransaction(transaction) })

        when:
        def response = restClient.get(path: TRANSACTIONS_URL)

        then:
        JSONAssert.assertEquals(objectMapper.writeValueAsString(testTransactions), response.data.toString(), JSONCompareMode.LENIENT)
    }

    def "Get transaction by id"() {
        setup:
        redisDao.createAccount(testAccountOne)
        redisDao.createAccount(testAccountTwo)
        redisDao.createTransaction(testTransaction)

        when:
        def response = restClient.get(path: transactionUrl(testTransaction.id))

        then:
        JSONAssert.assertEquals(objectMapper.writeValueAsString(testTransaction), objectMapper.writeValueAsString(response.data), JSONCompareMode.LENIENT)
    }
    
    def "Create transaction"() {
        setup:
        redisDao.createAccount(testAccountOne)
        redisDao.createAccount(testAccountTwo)

        when:
        def response = restClient.post(
                path: TRANSACTIONS_URL,
                requestContentType: APPLICATION_JSON,
                body: objectMapper.writeValueAsString(createTransactionRequest)
        )
        def expectedTransaction = new Transaction()
        expectedTransaction.setAmount(createTransactionRequest.getAmount())
        expectedTransaction.setSenderId(createTransactionRequest.getSenderAccountId())
        expectedTransaction.setRecipientId(createTransactionRequest.getRecipientAccountId())

        then:
        assert expectedTransaction.amount == response.data.amount
        assert expectedTransaction.senderId == response.data.senderId
        assert expectedTransaction.recipientId == response.data.recipientId
        assert response.data.id != null
        assert response.data.created != null
    }

    def "Get accounts transactions"() {
        setup:
        redisDao.createAccount(testAccountOne)
        redisDao.createAccount(testAccountTwo)
        def testTransactions = [Transaction.fromRequest(createTransactionRequest), Transaction.fromRequest(createTransactionRequest), Transaction.fromRequest(createTransactionRequest)]
        testTransactions.forEach({ transaction -> redisDao.createTransaction(transaction) })

        when:
        def response = restClient.get(path: accountsTransactionsUrl(testAccountOne.id))

        then:
        JSONAssert.assertEquals(objectMapper.writeValueAsString(testTransactions), objectMapper.writeValueAsString(response.data), JSONCompareMode.LENIENT)
    }

}

