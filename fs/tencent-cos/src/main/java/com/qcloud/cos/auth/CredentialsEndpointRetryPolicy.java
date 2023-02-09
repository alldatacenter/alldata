package com.qcloud.cos.auth;

public interface CredentialsEndpointRetryPolicy {
    CredentialsEndpointRetryPolicy NO_RETRY_POLICY = new CredentialsEndpointRetryPolicy() {
        @Override
        public boolean shouldRetry(int attemptedRetries, CredentialsEndpointRetryParameters retryParameters) {
            return false;
        }
    };

    /**
     * Checks whether a failed request should be retried;
     *
     * @param attemptedRetries The number of times the current request has been attempted.
     * @param retryParameters the requested error code and exception are wrapped in the retryParameters
     * @return True if the failed request should be retried
     */
    boolean shouldRetry(int attemptedRetries, CredentialsEndpointRetryParameters retryParameters);
}
