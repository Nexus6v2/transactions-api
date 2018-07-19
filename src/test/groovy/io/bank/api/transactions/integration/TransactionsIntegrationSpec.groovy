package io.bank.api.transactions.integration

import io.bank.api.transactions.model.Account
import io.bank.api.transactions.model.Transaction
import io.bank.api.transactions.model.dto.AccountDTO
import io.bank.api.transactions.model.dto.CreateTransactionRequest
import io.bank.api.transactions.model.dto.TransactionDTO
import io.bank.api.transactions.utils.Converter
import org.apache.http.HttpResponse
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.util.EntityUtils
import org.skyscreamer.jsonassert.JSONAssert
import org.skyscreamer.jsonassert.JSONCompareMode

class TransactionsIntegrationSpec extends BaseIntegrationSpec {

    def "Return 404 if there are no registered accounts"() {
        when:
        HttpResponse response = httpClient.execute(new HttpGet(ACCOUNTS_URL), null).get()

        then:
        assert response.getStatusLine().getStatusCode() == 404
    }

    def "Return 404 if there are no registered transactions"() {
        when:
        HttpResponse response = httpClient.execute(new HttpGet(TRANSACTIONS_URL), null).get()

        then:
        assert response.getStatusLine().getStatusCode() == 404
    }

    def "Return 500 if requested account does not exist"() {
        when:
        HttpResponse response = httpClient.execute(new HttpGet(getAccountUrl("invalidId")), null).get()

        then:
        assert response.getStatusLine().getStatusCode() == 500
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
        HttpPost request = new HttpPost(TRANSACTIONS_URL)
        request.addHeader(CONTENT_TYPE, APPLICATION_JSON)
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(transactionRequest)))

        HttpResponse response = httpClient.execute(request, null).get()

        then:
        assert response.getStatusLine().getStatusCode() == 500
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
        HttpPost request = new HttpPost(TRANSACTIONS_URL)
        request.addHeader(CONTENT_TYPE, APPLICATION_JSON)
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(transactionRequest)))

        HttpResponse response = httpClient.execute(request, null).get()

        then:
        assert response.getStatusLine().getStatusCode() == 500
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
        HttpPost request = new HttpPost(TRANSACTIONS_URL)
        request.addHeader(CONTENT_TYPE, APPLICATION_JSON)
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(transactionRequest)))

        HttpResponse response = httpClient.execute(request, null).get()

        then:
        assert response.getStatusLine().getStatusCode() == 500
    }

    def "Get all accounts"() {
        setup:
        def testAccounts = [Account.fromRequest(createAccountRequest), Account.fromRequest(createAccountRequest)]
        testAccounts.forEach({ account -> redisDao.createAccount(account).toBlocking().value() })
        testAccounts = testAccounts.collect {AccountDTO.fromAccount(it)}

        when:
        HttpResponse response = httpClient.execute(new HttpGet(ACCOUNTS_URL), null).get()

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(testAccounts),
                EntityUtils.toString(response.getEntity()),
                JSONCompareMode.LENIENT)
    }

    def "Get account by id"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()

        when:
        HttpResponse response = httpClient.execute(new HttpGet(getAccountUrl(testAccountOne.id)), null).get()

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(AccountDTO.fromAccount(testAccountOne)),
                EntityUtils.toString(response.getEntity()),
                JSONCompareMode.LENIENT)
    }

    def "Create account"() {
        when:
        HttpPost request = new HttpPost(ACCOUNTS_URL)
        request.addHeader(CONTENT_TYPE, APPLICATION_JSON)
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(createAccountRequest)))

        HttpResponse response = httpClient.execute(request, null).get()

        AccountDTO createdAccount = Converter.convertFromJson(EntityUtils.toString(response.getEntity()), AccountDTO.class)

        then:
        assert createAccountRequest.balance == createdAccount.getBalance()
        assert createAccountRequest.currencyCode == createdAccount.getCurrency()
    }

    def "Delete account"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()

        when:
        HttpResponse response = httpClient.execute(new HttpDelete(getAccountUrl(testAccountOne.id)), null).get()

        then:
        response.getStatusLine().getStatusCode() == 204
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
        HttpResponse response = httpClient.execute(new HttpGet(TRANSACTIONS_URL), null).get()

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(testTransactions),
                EntityUtils.toString(response.getEntity()),
                JSONCompareMode.LENIENT)
    }

    def "Get transaction by id"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()
        redisDao.createTransaction(testTransaction).toBlocking().value()

        when:
        HttpResponse response = httpClient.execute(new HttpGet(getTransactionUrl(testTransaction.id)), null).get()

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(TransactionDTO.fromTransaction(testTransaction)),
                EntityUtils.toString(response.getEntity()),
                JSONCompareMode.LENIENT)
    }
    
    def "Create transaction"() {
        setup:
        redisDao.createAccount(testAccountOne).toBlocking().value()
        redisDao.createAccount(testAccountTwo).toBlocking().value()

        when:
        HttpPost request = new HttpPost(TRANSACTIONS_URL)
        request.addHeader(CONTENT_TYPE, APPLICATION_JSON)
        request.setEntity(new StringEntity(objectMapper.writeValueAsString(createTransactionRequest)))

        HttpResponse response = httpClient.execute(request, null).get()

        TransactionDTO createdTransaction = Converter.convertFromJson(EntityUtils.toString(response.getEntity()), TransactionDTO.class)
        TransactionDTO expectedTransaction = TransactionDTO.fromTransaction(testTransaction)

        then:
        assert expectedTransaction.amount == createdTransaction.getAmount()
        assert expectedTransaction.currency == createdTransaction.getCurrency()
        assert expectedTransaction.senderId == createdTransaction.getSenderId()
        assert expectedTransaction.recipientId == createdTransaction.getRecipientId()
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
        HttpResponse response = httpClient.execute(new HttpGet(getAccountsTransactionsUrl(testAccountOne.id)), null).get()

        then:
        JSONAssert.assertEquals(
                objectMapper.writeValueAsString(testTransactions),
                EntityUtils.toString(response.getEntity()),
                JSONCompareMode.LENIENT)
    }

    def "Execute concurrent transaction's requests on the same account correctly"() {

    }
}