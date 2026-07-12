package com.alex.notiflow.contracts;

import java.util.concurrent.ThreadLocalRandom;

public enum MockFailureType {

    PERMANENT, RETRYABLE, RETRYABLE_ONCE;
    private static final MockFailureType[] VALUES = values();

    public static MockFailureType getRandomFailure() {
        int i = ThreadLocalRandom.current().nextInt(values().length);
        return VALUES[i];
    }
}
