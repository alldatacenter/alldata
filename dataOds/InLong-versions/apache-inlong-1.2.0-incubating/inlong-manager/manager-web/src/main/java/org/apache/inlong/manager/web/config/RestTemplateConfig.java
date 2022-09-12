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

package org.apache.inlong.manager.web.config;

import java.nio.charset.StandardCharsets;
import java.util.List;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.util.ObjectUtils;
import org.springframework.web.client.DefaultResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

@Data
@Configuration
@ConditionalOnMissingBean(RestTemplate.class)
@ConfigurationProperties(prefix = "common.http-client")
public class RestTemplateConfig {

    /**
     * Max total
     */
    @Value("${common.http-client.maxTotal}")
    private int maxTotal;
    /**
     * Concurrency
     */
    @Value("${common.http-client.defaultMaxPerRoute}")
    private int defaultMaxPerRoute;

    @Value("${common.http-client.validateAfterInactivity}")
    private int validateAfterInactivity;

    /**
     * Time to connect to the server (successful handshake), timeout throws connect timeout
     */
    @Value("${common.http-client.connectionTimeout}")
    private int connectionTimeout;
    /**
     * The time for the server to return data (response), timeout throws read timeout
     */
    @Value("${common.http-client.readTimeout}")
    private int readTimeout;
    /**
     * Get the timeout time of the connection from the connection pool,
     * and throw ConnectionPoolTimeoutException when timeout
     */
    @Value("${common.http-client.connectionRequestTimeout}")
    private int connectionRequestTimeout;

    @Bean
    public PoolingHttpClientConnectionManager httpClientConnectionManager() {
        // Support HTTP, HTTPS
        Registry<ConnectionSocketFactory> registry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", SSLConnectionSocketFactory.getSocketFactory())
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
