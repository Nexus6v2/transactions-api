package io.bank.api.transactions.utils;

import java.util.UUID;

public class CommonUtils {
    
    /**
     * Returns just the first part of common UUID, for the sake of simplicity
     */
    public static String getShortId() {
        return UUID.randomUUID().toString().split("-")[0];
    }
    
}
