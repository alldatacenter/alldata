package com.qcloud.cos.auth;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.qcloud.cos.exception.CosClientException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InstanceCredentialsFetcher extends HttpCredentialsFetcher {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceCredentialsFetcher.class);

    private static final ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.PASCAL_CASE_TO_CAMEL_CASE);
    }

    private static class CAMSecurityCredentials {
        public String tmpSecretId;
        public String tmpSecretKey;
        public String token;
        public long expiredTime;
        public String expiration;
        public String code;
    }

    public InstanceCredentialsFetcher(CredentialsEndpointProvider cosCredentialsEndpointProvider) {
        super(cosCredentialsEndpointProvider);
    }

    @Override
    public COSCredentials parse(String credentialsResponse) throws CosClientException {
        if (null == credentialsResponse || credentialsResponse.isEmpty()) {
            return null;
        }

        try {
            CAMSecurityCredentials camSecurityCredentials = mapper.readValue(credentialsResponse,
                    CAMSecurityCredentials.class);
            if (null == camSecurityCredentials) {
                return null;
            }

            return new InstanceProfileCredentials(camSecurityCredentials.tmpSecretId,
                    camSecurityCredentials.tmpSecretKey, camSecurityCredentials.token,
                    camSecurityCredentials.expiredTime);
        } catch (JsonProcessingException e) {
            LOG.error("Parse the instance credentials response failed.", e);
        }

        return null;
    }
}
