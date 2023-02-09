package com.qcloud.cos.auth;

import com.qcloud.cos.exception.CosClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;

public abstract class HttpCredentialsFetcher implements COSCredentialsFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(HttpCredentialsFetcher.class);

    private final CredentialsEndpointProvider cosCredentialsEndpointProvider;

    public abstract COSCredentials parse(String credentialsResponse) throws CosClientException;

    protected HttpCredentialsFetcher(CredentialsEndpointProvider cosCredentialsEndpointProvider) {
        this.cosCredentialsEndpointProvider = cosCredentialsEndpointProvider;
    }

    @Override
    public COSCredentials fetch() throws CosClientException {
        if (null == this.cosCredentialsEndpointProvider) {
            String errorMsg = "The cos credentials endpoint provider is not specified.";
            LOG.error(errorMsg);
            throw new CosClientException(errorMsg);
        }

        try {
            String credentialsResponse =
                    InstanceCredentialsUtils.getInstance().readResource(
                            this.cosCredentialsEndpointProvider.getCredentialsEndpoint(),
                            this.cosCredentialsEndpointProvider.getRetryPolicy(),
                            this.cosCredentialsEndpointProvider.getHeaders());
            return parse(credentialsResponse);
        } catch (URISyntaxException e) {
            throw new CosClientException("The cos credentials uri is invalid.", e);
        } catch (IOException e) {
            String exceptionMessage = String.format("The COSCredentialsFetcher [%s] fetch an exception.",
                    this.getClass().getName());
            throw new CosClientException(exceptionMessage, e);
        }
    }

    @Override
    public COSCredentials fetch(int retryTimes) throws CosClientException {
        for (int i = 1; i <= retryTimes; i++) {
            try {
                return this.fetch();
            } catch (CosClientException e) {
                String errorMsg = String.format("The COSCredentialsFetcher [%s] fetch failed, retry: %d/%d, exception:",
                                                this.getClass().getName(), i, retryTimes);
                LOG.error(errorMsg, e);
            }
        }
        throw new CosClientException("Failed to fetch the CosCredentials from a instance metadata service: max retry " +
                "times exceeded.");
    }
}
