package com.qcloud.cos.auth;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public abstract class CredentialsEndpointProvider {
    public abstract URI getCredentialsEndpoint() throws URISyntaxException, IOException;

    public CredentialsEndpointRetryPolicy getRetryPolicy() {
        return CredentialsEndpointRetryPolicy.NO_RETRY_POLICY;
    }

    public Map<String, String> getHeaders() {
        return new HashMap<>();
    }
}
