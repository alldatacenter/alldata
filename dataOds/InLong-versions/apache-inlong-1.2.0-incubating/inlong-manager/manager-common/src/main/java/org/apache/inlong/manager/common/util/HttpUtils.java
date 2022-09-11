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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * HTTP utils
 */
@Component
@Slf4j
public class HttpUtils {

    private static final Gson gson = new GsonBuilder().create(); // thread safe
    @Autowired
    private RestTemplate restTemplate;

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
     * Send an HTTP request
     */
    public <T> T request(String url, HttpMethod method, String param, HttpHeaders header, Class<T> cls)
            throws Exception {
        // Set request header parameters
        ResponseEntity<String> exchange;
        try {
            HttpEntity<String> request = new HttpEntity<>(param, header);
            log.debug("send http request to {}, param {}", url, param);
            exchange = restTemplate.exchange(url, method, request, String.class);

            String body = exchange.getBody();
            HttpStatus statusCode = exchange.getStatusCode();
            if (!statusCode.is2xxSuccessful()) {
                log.error("request error for {}, status code {}, body {}", url, statusCode, body);
            }

            log.debug("response from {}, status code {}", url, statusCode);
            return gson.fromJson(exchange.getBody(), cls);
        } catch (RestClientException e) {
            log.error(" do request for {} exception {} ", url, e.getMessage());
            throw e;
        }
    }

    /**
     * Send an HTTP request
     */
    public <T> T request(String url, HttpMethod httpMethod, Object requestBody, HttpHeaders header,
            ParameterizedTypeReference<T> typeReference) {
        if (log.isDebugEnabled()) {
            log.debug("call {}, request body {}", url, gson.toJson(requestBody));
        }

        HttpEntity<Object> requestEntity = new HttpEntity<>(requestBody, header);
        ResponseEntity<T> response = restTemplate.exchange(url, httpMethod, requestEntity, typeReference);

        if (log.isDebugEnabled()) {
            log.debug("call {}, status code {}", url, response.getStatusCode());
        }

        Preconditions.checkTrue(response.getStatusCode().is2xxSuccessful(), "Request failed");
        return response.getBody();
    }

    public <T> T postRequest(String url, Object params, HttpHeaders header,
            ParameterizedTypeReference<T> typeReference) {
        return request(url, HttpMethod.POST, params, header, typeReference);
    }

    public <T> T getRequest(String url, Map<String, Object> params, HttpHeaders header,
            ParameterizedTypeReference<T> typeReference) {
        return request(buildUrlWithQueryParam(url, params), HttpMethod.GET, null, header, typeReference);
    }

    private String buildUrlWithQueryParam(String url, Map<String, Object> params) {
        if (params == null) {
            return url;
        }
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
        params.entrySet().stream().filter(e -> e.getValue() != null)
                .forEach(e -> builder.queryParam(e.getKey(), e.getValue()));
        return builder.build(false).toUriString();
    }

}
