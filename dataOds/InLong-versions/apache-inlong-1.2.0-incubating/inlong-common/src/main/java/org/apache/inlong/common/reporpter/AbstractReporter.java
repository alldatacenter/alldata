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

package org.apache.inlong.common.reporpter;

import com.google.gson.Gson;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractReporter<T> {

    public static final Logger LOGGER = LoggerFactory.getLogger(AbstractReporter.class);

    public static final String AGENT_HTTP_APPLICATION_JSON = "application/json";

    private static final Gson gson = new Gson();

    private static final int DEFAULT_CORE_POOL_SIZE = 1;

    private static final int DEFAULT_MAX_POOL_SIZE = 2;

    private static final int DEFAULT_SYNC_SEND_QUEUE_SIZE = 10000;

    private static CloseableHttpClient httpClient;

    private final Class<?> clazz = Response.class;

    private ThreadPoolExecutor pool;

    private String serverUrl;

    public AbstractReporter(String serverUrl) {
        this(serverUrl, DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE,
                DEFAULT_SYNC_SEND_QUEUE_SIZE, null);
    }

    public AbstractReporter(CloseableHttpClient httpClient, String serverUrl) {
        this(httpClient, serverUrl, DEFAULT_CORE_POOL_SIZE, DEFAULT_MAX_POOL_SIZE,
                DEFAULT_SYNC_SEND_QUEUE_SIZE, null);
    }

    public AbstractReporter(String serverUrl, int corePoolSize, int maximumPoolsize,
            int syncSendQueueSize,
            RejectedExecutionHandler rejectedExecutionHandler) {
        this.serverUrl = serverUrl;
        if (httpClient == null) {
            RequestConfig requestConfig = RequestConfig.custom().build();
            HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
            httpClientBuilder.setDefaultRequestConfig(requestConfig);
            httpClient = httpClientBuilder.build();
        }
        if (rejectedExecutionHandler == null) {
            rejectedExecutionHandler = new ThreadPoolExecutor.DiscardOldestPolicy();
        }
        pool = new ThreadPoolExecutor(corePoolSize, maximumPoolsize,
                60, TimeUnit.SECONDS, new LinkedBlockingDeque<>(syncSendQueueSize),
                Executors.defaultThreadFactory(), rejectedExecutionHandler);
    }

    public AbstractReporter(CloseableHttpClient httpClient, String serverUrl, int corePoolSize,
            int maximumPoolSize,
            int syncSendQueueSize,
            RejectedExecutionHandler rejectedExecutionHandler) {
        this(serverUrl, corePoolSize, maximumPoolSize, syncSendQueueSize, rejectedExecutionHandler);
        this.httpClient = httpClient;
    }

    /**
     * Report data by sync.
     */
    public Response syncReportData(T data, String serverUrl) throws Exception {
        if (StringUtils.isEmpty(serverUrl)) {
            LOGGER.warn("Report config log server url is empty, so config log can not be "
                    + "reported!");
            return null;
        }
        HttpPost httpPost = new HttpPost(serverUrl);
        String returnStr = null;
        try {
            StringEntity stringEntity = new StringEntity(gson.toJson(data));
            stringEntity.setContentType(AGENT_HTTP_APPLICATION_JSON);
            httpPost.setEntity(stringEntity);
            returnStr = executeHttpPost(httpPost);
            return parse(returnStr);
        } catch (Exception e) {
            LOGGER.error("syncReportData has exception returnStr = {}, e:", returnStr, e);
            throw e;
        }
    }

    /**
     * Report data by sync.
     */
    public Response syncReportData(T data) throws Exception {
        return this.syncReportData(data, serverUrl);
    }

    public String executeHttpPost(HttpPost httpPost) throws Exception {
        CloseableHttpResponse response = httpClient.execute(httpPost);
        if (response == null) {
            return null;
        }
        return EntityUtils.toString(response.getEntity());
    }

    /**
     * Report data by async.
     */
    public Future<Response>  asyncReportData(T data, String serverUrl) {
        CompletableFuture<Response> completableFuture = new CompletableFuture<>();

        if (pool != null) {
            pool.execute(new RunTask(completableFuture, data, serverUrl));
        } else {
            completableFuture.completeExceptionally(new Exception("Send pool is null!"));
        }

        return completableFuture;
    }

    /**
     * Report data by async.
     */
    public Future<Response> asyncReportData(T data) {
        return asyncReportData(data, serverUrl);
    }

    /**
     * Parse json data.
     */
    public Response parse(String json) throws Exception {

        if (StringUtils.isEmpty(json)) {
            return null;
        }

        return gson.fromJson(json, Response.class);
    }

    class RunTask implements Runnable {

        private CompletableFuture<Response> completableFuture;

        private T data;

        private String url;

        public RunTask(CompletableFuture<Response> completableFuture, T data, String url) {
            this.completableFuture = completableFuture;
            this.data = data;
            this.url = url;
        }

        public void run() {
            try {
                completableFuture.complete(syncReportData(data, url));
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        }
    }
}
