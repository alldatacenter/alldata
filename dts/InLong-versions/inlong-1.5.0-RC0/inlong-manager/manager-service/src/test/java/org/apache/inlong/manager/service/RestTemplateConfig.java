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

package org.apache.inlong.manager.service;

import lombok.Data;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.ConnectionKeepAliveStrategy;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.protocol.HttpContext;
import org.apache.inlong.common.constant.ProtocolType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Test class for rest template config.
 */
@Data
@Configuration
@ConditionalOnMissingBean(RestTemplate.class)
public class RestTemplateConfig {

    /**
     * Max total
     */
    private final int maxTotal = 5000;
    /**
     * Concurrency
     */
    private final int defaultMaxPerRoute = 2000;

    private final int validateAfterInactivity = 5000;

    /**
     * Time to connect to the server (successful handshake), timeout throws connect timeout
     */
    private final int connectionTimeout = 3000;
    /**
     * The time for the server to return data (response), timeout throws read timeout
     */
    private final int readTimeout = 10000;
    /**
     * Get the timeout time of the connection from the connection pool,
     * and throw ConnectionPoolTimeoutException when timeout
     */
    private final int connectionRequestTimeout = 3000;

    @Bean
    public PoolingHttpClientConnectionManager httpClientConnectionManager() {
        // Support HTTP, HTTPS
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register(ProtocolType.HTTP, PlainConnectionSocketFactory.getSocketFactory())
                .register(ProtocolType.HTTPS, SSLConnectionSocketFactory.getSocketFactory())
                .build();
        PoolingHttpClientConnectionManager httpClientConnectionManager = new PoolingHttpClientConnectionManager(
                registry);
        httpClientConnectionManager.setMaxTotal(maxTotal);
        httpClientConnectionManager.setDefaultMaxPerRoute(defaultMaxPerRoute);
        httpClientConnectionManager.setValidateAfterInactivity(validateAfterInactivity);

        return httpClientConnectionManager;
    }

    @Bean
    public HttpClient httpClient() {
        return HttpClientBuilder.create()
                .setConnectionManager(httpClientConnectionManager())
                .setKeepAliveStrategy(CustomConnectionKeepAliveStrategy.INSTANCE)
                .build();
    }

    @Bean
    public HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
        // httpClient connection configuration
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(httpClient());
        // Time to connect to the server (successful handshake), timeout throws connect timeout
        factory.setConnectTimeout(connectionTimeout);
        // The time for the server to return data (response), timeout throws read timeout
        factory.setReadTimeout(readTimeout);
        // Get the timeout of the connection from the connection pool
        factory.setConnectionRequestTimeout(connectionRequestTimeout);
        return factory;
    }

    @Bean
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        restTemplate.setErrorHandler(new DefaultResponseErrorHandler());
        setRestTemplateEncode(restTemplate);
        return restTemplate;
    }

    private void setRestTemplateEncode(RestTemplate restTemplate) {
        if (null == restTemplate || ObjectUtils.isEmpty(restTemplate.getMessageConverters())) {
            return;
        }

        List<HttpMessageConverter<?>> messageConverters = restTemplate.getMessageConverters();
        for (int i = 0; i < messageConverters.size(); i++) {
            HttpMessageConverter<?> httpMessageConverter = messageConverters.get(i);
            if (httpMessageConverter.getClass().equals(StringHttpMessageConverter.class)) {
                messageConverters.set(i, new StringHttpMessageConverter(StandardCharsets.UTF_8));
            }
        }
    }

    public static class CustomConnectionKeepAliveStrategy implements ConnectionKeepAliveStrategy {

        public static CustomConnectionKeepAliveStrategy INSTANCE = new CustomConnectionKeepAliveStrategy();

        private CustomConnectionKeepAliveStrategy() {
        }

        @Override
        public long getKeepAliveDuration(HttpResponse response, HttpContext context) {
            return 30 * 1000;
        }
    }

}
