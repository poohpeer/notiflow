package com.alex.notiflow.contracts;

public record ProviderResult(
        boolean sent,
        FailureType failureType,
        String reason
) {
    public static ProviderResult success() {
        return new ProviderResult(true, FailureType.NONE, null);
    }

    public static ProviderResult retryable(String reason) {
        return new ProviderResult(false, FailureType.RETRYABLE, reason);
    }

    public static ProviderResult permanent(String reason) {
        return new ProviderResult(false, FailureType.PERMANENT, reason);
    }
}
