package com.qcloud.cos.auth;

public class InstanceCredentialsRetryPolicy implements CredentialsEndpointRetryPolicy {
    private static final int MAX_RETRIES = 5;

    private static InstanceCredentialsRetryPolicy instance;

    public static InstanceCredentialsRetryPolicy getInstance() {
        if (null == instance) {
            instance = new InstanceCredentialsRetryPolicy();
        }

        return instance;
    }

    private InstanceCredentialsRetryPolicy() {
    }

    @Override
    public boolean shouldRetry(int attemptedRetries, CredentialsEndpointRetryParameters retryParameters) {
        return false;
    }
}
