package com.obs.services.internal.ext;

import com.obs.services.ObsConfiguration;

public class ExtObsConfiguration extends ObsConfiguration {

    private boolean retryOnConnectionFailureInOkhttp;

    // times for retryOnRetryOnUnexpectedEndException;
    private int maxRetryOnUnexpectedEndException;

    public ExtObsConfiguration() {
        super();
        this.retryOnConnectionFailureInOkhttp = ExtObsConstraint.DEFAULT_RETRY_ON_CONNECTION_FAILURE_IN_OKHTTP;
        this.maxRetryOnUnexpectedEndException = ExtObsConstraint.DEFAULT_MAX_RETRY_ON_UNEXPECTED_END_EXCEPTION;
    }

    public boolean isRetryOnConnectionFailureInOkhttp() {
        return retryOnConnectionFailureInOkhttp;
    }

    public void retryOnConnectionFailureInOkhttp(boolean retryOnConnectionFailureInOkhttp) {
        this.retryOnConnectionFailureInOkhttp = retryOnConnectionFailureInOkhttp;
    }

    public int getMaxRetryOnUnexpectedEndException() {
        return maxRetryOnUnexpectedEndException;
    }

    public void setMaxRetryOnUnexpectedEndException(int maxRetryOnUnexpectedEndException) {
        this.maxRetryOnUnexpectedEndException = maxRetryOnUnexpectedEndException;
    }
}
