/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * HTTP utils
 */
@Slf4j
public class HttpUtils {

    private static final Gson GSON = new GsonBuilder().create(); // thread safe

    /**
     * Check whether the host and port can connect
     *
     * @param host target host address
     * @param port target port
     * @param connectTimeout connect timeout
     * @param timeUnit time unit of timeout
     * @return true if connect successfully, false if connect failed
     */
    public static boolean checkConnectivity(String host, int port, int connectTimeout, TimeUnit timeUnit) {
        InetSocketAddress socketAddress = new InetSocketAddress(host, port);
        Socket socket = new Socket();
        try {
            socket.connect(socketAddress, (int) timeUnit.toMillis(connectTimeout));
            return socket.isConnected();
        } catch (IOException e) {
            log.error(String.format("%s:%s connected failed with err msg:%s", host, port, e.getMessage()));
            return false;
        } finally {
            try {
                socket.close();
            } catch (IOException e) {
                log.warn("close connection from {}:{} failed", host, port, e);
            }
        }
    }

    /**
     * Send an HTTP request by the given rest template.
     */
    public static <T> T request(RestTemplate restTemplate, String url, HttpMethod method,
            String param, HttpHeaders header, Class<T> cls) throws Exception {
        ResponseEntity<String> exchange;
        try {
            HttpEntity<String> request = new HttpEntity<>(param, header);
            log.debug("send request to {}, param {}", url, param);
            exchange = restTemplate.exchange(url, method, request, String.class);
            String body = exchange.getBody();
            HttpStatus statusCode = exchange.getStatusCode();
            if (!statusCode.is2xxSuccessful()) {
                log.error("request error for {}, status code {}, body {}", url, statusCode, body);
            }

            log.debug("response from {}, status code {}", url, statusCode);
            return GSON.fromJson(exchange.getBody(), cls);
        } catch (RestClientException e) {
            log.error("request for {} exception {} ", url, e.getMessage());
            throw e;
        }
    }

    /**
     * Send an HTTP request
     */
    public static <T> T request(RestTemplate restTemplate, String url, HttpMethod httpMethod, Object requestBody,
            HttpHeaders header, ParameterizedTypeReference<T> typeReference) {
        if (log.isDebugEnabled()) {
            log.debug("begin request to {} by request body {}", url, GSON.toJson(requestBody));
        }

        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, header);
        ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, requestEntity, typeReference);

        log.debug("success request to {}, status code {}", url, response.getStatusCode());
        Preconditions.expectTrue(response.getStatusCode().is2xxSuccessful(), "Request failed");
        return response.getBody();
    }

    /**
     * Send GET request to the specified URL.
     */
    public static <T> T getRequest(RestTemplate restTemplate, String url, Map<String, Object> params,
            HttpHeaders header, ParameterizedTypeReference<T> typeReference) {
        return request(restTemplate, buildUrlWithQueryParam(url, params), HttpMethod.GET, null, header, typeReference);
    }

    /**
     * Send PUT request to the specified URL.
     */
    public static <T> T putRequest(RestTemplate restTemplate, String url, Object params, HttpHeaders header,
            ParameterizedTypeReference<T> typeReference) {
        return request(restTemplate, url, HttpMethod.PUT, params, header, typeReference);
    }

    /**
     * Send POST request to the specified URL.
     */
    public static <T> T postRequest(RestTemplate restTemplate, String url, Object params, HttpHeaders header,
            ParameterizedTypeReference<T> typeReference) {
        return request(restTemplate, url, HttpMethod.POST, params, header, typeReference);
    }

    private static String buildUrlWithQueryParam(String url, Map<String, Object> params) {
        if (params == null) {
            return url;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        params.entrySet().stream().filter(e -> e.getValue() != null)
                .forEach(e -> builder.queryParam(e.getKey(), e.getValue()));
        return builder.build(false).toUriString();
    }

}
