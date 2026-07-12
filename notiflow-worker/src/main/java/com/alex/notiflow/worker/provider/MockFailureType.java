package com.alex.notiflow.worker.provider;

import java.util.concurrent.ThreadLocalRandom;

public enum MockFailureType {

    PERMANENT, RETRYABLE, RETRYABLE_ONCE;

    public static final MockFailureType[] VALUES = MockFailureType.values();

    public static MockFailureType getRandomFailure() {
        MockFailureType[] values = VALUES;
        var index = ThreadLocalRandom.current().nextInt(values.length);
        return values[index];
    }
}
