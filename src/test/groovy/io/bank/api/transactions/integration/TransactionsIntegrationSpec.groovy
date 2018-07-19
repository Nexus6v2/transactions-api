package io.bank.api.transactions.integration

import io.bank.api.transactions.model.Account
import io.bank.api.transactions.model.Transaction
import io.bank.api.transactions.model.dto.AccountDTO
import io.bank.api.transactions.model.dto.CreateTransactionRequest
import io.bank.api.transactions.model.dto.TransactionDTO
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class TransactionsIntegrationSpec extends BaseIntegrationSpec {

    def "Return 500 if requested account does not exist"() {
        when:
        def response = restClient.get(path: accountUrl("invalidId"))

        then:
        assert response.status == 500
    }

    def "Return 500 if sender account does not exist"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()
        CreateTransactionRequest transactionRequest = new CreateTransactionRequest()
                .setAmount(100)
                .setCurrencyCode("USD")
                .setSenderAccountId("invalidId")
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
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()
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

    def "Return 500 if transaction has invalid currency"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()
        CreateTransactionRequest transactionRequest = new CreateTransactionRequest()
                .setAmount(100)
                .setCurrencyCode("US_DOLLAR")
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
        def testAccounts = [Account.fromRequest(createAccountRequest), Account.fromRequest(createAccountRequest)]
        testAccounts.forEach({ account -> redisDao.createAccount(account).toBlocking().value() })
        testAccounts = testAccounts.collect {AccountDTO.fromAccount(it)}

        when:
        def response = restClient.get(path: ACCOUNTS_URL)

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(testAccounts),
                objectMapper.writeValueAsString(response.data),
                JSONCompareMode.LENIENT)
    }

    def "Get account by id"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()

        when:
        def response = restClient.get(path: accountUrl(testAccountOne.id))

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(AccountDTO.fromAccount(testAccountOne)),
                objectMapper.writeValueAsString(response.data),
                JSONCompareMode.LENIENT)
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
        assert createAccountRequest.currencyCode == response.data.currency
    }

    def "Delete account"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()

        when:
        def response = restClient.delete(path: accountUrl(testAccountOne.id))

        then:
        response.status == 204
    }

    def "Get all transactions"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()
        def testTransactions = [Transaction.fromRequest(createTransactionRequest),
                                Transaction.fromRequest(createTransactionRequest),
                                Transaction.fromRequest(createTransactionRequest)]
        testTransactions.forEach({ transaction -> redisDao.createTransaction(transaction).toBlocking().value() })
        testTransactions = testTransactions.collect {TransactionDTO.fromTransaction(it)}

        when:
        def response = restClient.get(path: TRANSACTIONS_URL)

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(testTransactions),
                objectMapper.writeValueAsString(response.data),
                JSONCompareMode.LENIENT)
    }

    def "Get transaction by id"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()
        redisDao.createTransaction(testTransaction).toBlocking().value()

        when:
        def response = restClient.get(path: transactionUrl(testTransaction.id))

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(TransactionDTO.fromTransaction(testTransaction)),
                objectMapper.writeValueAsString(response.data),
                JSONCompareMode.LENIENT)
    }
    
    def "Create transaction"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()

        when:
        def response = restClient.post(
                path: TRANSACTIONS_URL,
                requestContentType: APPLICATION_JSON,
                body: objectMapper.writeValueAsString(createTransactionRequest)
        )
        def expected = TransactionDTO.fromTransaction(testTransaction)

        then:
        assert expected.amount == response.data.amount
        assert expected.currency == response.data.currency
        assert expected.senderId == response.data.senderId
        assert expected.recipientId == response.data.recipientId
    }

    def "Get accounts transactions"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()
        def testTransactions = [Transaction.fromRequest(createTransactionRequest),
                                Transaction.fromRequest(createTransactionRequest),
                                Transaction.fromRequest(createTransactionRequest)]
        testTransactions.forEach({ transaction -> redisDao.createTransaction(transaction).toBlocking().value() })
        testTransactions = testTransactions.collect {TransactionDTO.fromTransaction(it)}

        when:
        def response = restClient.get(path: accountsTransactionsUrl(testAccountOne.id))

        then:
        JSONAssert.assertEquals(objectMapper.writeValueAsString(testTransactions), objectMapper.writeValueAsString(response.data), JSONCompareMode.LENIENT)
    }

}

