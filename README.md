# transactions-api


## How to run
`./gradlew run --no-daemon`

API would be available on default port `8080`. Redis embedder server would be started on `6379`.

To terminate just use SIGINT (Ctrl+C).

## How to test
`./gradlew clean test`

## API
* `/health` - healthcheck 
1. Accounts
    * `/accounts #GET` - get list of all existing accounts
    * `/accounts #POST` - create new account. 
        * Note: assuming that account's balance is in minor currency units. 
        * Request body: 
        ```json
        { 
            "balance": 10000000,
            "currencyCode": "USD"
        }
        ```
    * `/accounts/:accountId #GET` - get specific account by it's id
    * `/accounts/:accountId #DELETE` - delete existing account
    * `/accounts/:accountId/transactions` - get all transactions of this account
    
* Transactions
    * `/transactions #GET` - get list of all executed transactions
    * `/transactions #POST` - execute new transaction.
        * Note: assuming that transaction amount represented in minor units. 
        * Note: transactions between accounts with different currencies are not supported. 
        * Request body: 
        ```json
        { 
            "senderAccountId": "2b69ffa4", 
            "recipientAccountId": "473dc600",
            "amount": 100000
        }
        ```
    * `/transactions/:transactionId #GET` - get transaction info by it's id

## Frameworks and libraries

* `Vert.x` - lightweight Netty-based framework for building reactive applications
* `Lombok` - generating boilerplate Java code (accessors, checked exceptions, Object and Builder methods) in compile-time 
* `RxJava` - composing asynchronous and event-based programs by using observable sequences
* `Spock` - powerful declarative testing Groovy framework
* `Lettuce` - awesome Redis client with RxJava support
* `embedded-redis` - simple embedded Redis server