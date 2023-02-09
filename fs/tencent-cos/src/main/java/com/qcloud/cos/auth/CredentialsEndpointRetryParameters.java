package com.qcloud.cos.auth;

public class CredentialsEndpointRetryParameters {
    private final Integer statusCode;
    private final Exception exception;

    public static class Builder {
        private Integer statusCode;
        private Exception exception;

        private Builder() {
            this(null, null);
        }

        public Builder(Integer statusCode, Exception exception) {
            this.statusCode = statusCode;
            this.exception = exception;
        }

        public Builder withStatusCode(Integer statusCode) {
            this.statusCode = statusCode;
            return this;
        }

        public Builder withException(Exception exception) {
            this.exception = exception;
            return this;
        }

        public CredentialsEndpointRetryParameters build() {
            return new CredentialsEndpointRetryParameters(this);
        }
    }

    public CredentialsEndpointRetryParameters(Builder builder) {
        this.statusCode = builder.statusCode;
        this.exception = builder.exception;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public Exception getException() {
        return exception;
    }

    public static Builder builder() {
        return new Builder();
    }
}
