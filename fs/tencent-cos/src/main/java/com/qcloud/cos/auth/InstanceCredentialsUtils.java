package com.qcloud.cos.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.qcloud.cos.exception.CosClientException;
import com.qcloud.cos.exception.CosServiceException;
import com.qcloud.cos.utils.IOUtils;
import com.qcloud.cos.utils.Jackson;
import com.qcloud.cos.utils.VersionInfoUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The utils for CPM and CVM credentials fetcher
 */
public class InstanceCredentialsUtils {
    private static final Logger LOG = LoggerFactory.getLogger(InstanceCredentialsUtils.class);

    private static InstanceCredentialsUtils instance = new InstanceCredentialsUtils(ConnectionUtils.getInstance());
    private static final String USER_AGENT = VersionInfoUtils.getUserAgent();

    private final ConnectionUtils connectionUtils;

    private InstanceCredentialsUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }

    public static InstanceCredentialsUtils getInstance() {
        return instance;
    }

    public String readResource(URI endpoint) throws IOException {
        return readResource(endpoint, CredentialsEndpointRetryPolicy.NO_RETRY_POLICY, null);
    }

    public String readResource(URI endpoint, CredentialsEndpointRetryPolicy retryPolicy, Map<String, String> headers) throws IOException {
        int retriesAttempted = 0;
        InputStream inputStream = null;
        Map<String, String> fullHeader = addDefaultHeader(headers);

        while (true) {
            try {
                HttpURLConnection connection = this.connectionUtils.connectToEndpoint(endpoint, fullHeader);
                int statusCode = connection.getResponseCode();

                if (statusCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    return IOUtils.toString(inputStream);
                } else if (statusCode == HttpURLConnection.HTTP_NOT_FOUND) {
                    String errorMsg = "The requested metadata is not found at " + connection.getURL();
                    LOG.error(errorMsg);
                    throw new CosClientException(errorMsg);
                } else {
                    LOG.error("The response status code is:" + statusCode);
                    inputStream = connection.getErrorStream();
                    String errorMessage = null;
                    String errorCode = null;
                    if (null != inputStream) {
                        String errorResponse = IOUtils.toString(inputStream);
                        LOG.error("errorResponse:" + errorResponse);
                        try {
                            JsonNode node = Jackson.jsonNodeOf(errorResponse);
                            JsonNode code = node.get("code");
                            JsonNode message = node.get("message");
                            if (null != code) {
                                errorCode = code.asText();
                            }
                            if (null != message) {
                                errorMessage = message.asText();
                            }
                        } catch (Exception exception) {
                            LOG.error("Unable to parse errorResponse:" + errorResponse, exception);
                        }
                    }
                    if (!retryPolicy.shouldRetry(retriesAttempted++,
                            CredentialsEndpointRetryParameters.builder().withStatusCode(statusCode).build())) {
                        CosServiceException cosServiceException =
                                new CosServiceException(connection.getResponseMessage());
                        if (null != errorMessage) {
                            cosServiceException.setErrorMessage(errorMessage);
                        }
                        if (null != errorCode) {
                            cosServiceException.setErrorCode(errorCode);
                        }
                        throw cosServiceException;
                    }
                }
            } catch (IOException e) {
                LOG.error("An IOException occurred, service endpoint:" + endpoint + ", exception:", e);
                if (!retryPolicy.shouldRetry(retriesAttempted++,
                        CredentialsEndpointRetryParameters.builder().withException(e).build())) {
                    throw e;
                }
            } finally {
                IOUtils.closeQuietly(inputStream, LOG);
            }
        }
    }

    private Map<String, String> addDefaultHeader(Map<String, String> headers) {
        Map<String, String> fullHeader = new HashMap<String, String>();
        if (headers != null) {
            fullHeader.putAll(headers);
        }

        putIfAbsent(fullHeader,"User-Agent", USER_AGENT);
        putIfAbsent(fullHeader,"Accept", "*/*");
        putIfAbsent(fullHeader,"Connection", "keep-alive");

        return fullHeader;
    }

    private <K, V> void putIfAbsent(Map<K, V> map, K key, V value) {
        if (map.get(key) == null) {
            map.put(key, value);
        }
    }
}
