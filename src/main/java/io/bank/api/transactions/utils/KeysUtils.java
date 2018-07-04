package io.bank.api.transactions.utils;

import static java.lang.String.format;

public class KeysUtils {
    public static final String ACCOUNT_KEY_PATTERN = "account:*";
    public static final String TRANSACTION_KEY_PATTERN = "transaction:*";
    
    private static final String ACCOUNT = "account";
    private static final String TRANSACTION = "transaction";
    
    /**
     * Generates a key for Redis Hash in format of "account:id"
     */
    public static String getAccountKey(String id) {
        return format("%s:%s", ACCOUNT, id);
    }
    
    /**
     * Generates a key for Redis Hash in format of "transaction:id"
     */
    public static String getTransactionKey(String id) {
        return format("%s:%s", TRANSACTION, id);
    }
}
