/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sort.standalone.sink.elasticsearch;

import java.io.IOException;
import java.util.Arrays;
import java.util.function.BiConsumer;

import org.apache.flume.lifecycle.LifecycleState;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.bulk.BulkResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;

/**
 * EsOutputChannel
 */
public class EsOutputChannel extends Thread {

    public static final Logger LOG = InlongLoggerFactory.getLogger(EsOutputChannel.class);

    private LifecycleState status;
    private EsSinkContext context;
    private RestHighLevelClient esClient;
    private BulkProcessor bulkProcessor;

    /**
     * Constructor
     * 
     * @param context
     */
    public EsOutputChannel(EsSinkContext context) {
        super(context.getTaskName());
        this.context = context;
        this.status = LifecycleState.IDLE;
    }

    /**
     * init
     */
    public void init() {
        if (initEsclient()) {
            initBulkprocessIfNeed();
        }
    }

    /**
     * getBulkProcessor
     *
     * @return
     */
    public BulkProcessor getBulkProcessor() {
        if (bulkProcessor == null) {
            if (initEsclient()) {
                initBulkprocessIfNeed();
            }
        }
        return bulkProcessor;
    }

    /**
     * initEsclient
     * 
     * @return
     */
    private boolean initEsclient() {
        try {
            if (esClient == null || !esClient.ping(RequestOptions.DEFAULT)) {
                String userName = context.getUsername();
                String password = context.getPassword();

                HttpHost[] httpHosts = context.getHttpHosts();
                LOG.info("initEsclient:url:{},user:{},password:{}",
                        Arrays.asList(httpHosts), userName, password);

                RestClientBuilder builder = RestClient.builder(httpHosts);
                final CredentialsProvider provider = new BasicCredentialsProvider();
                provider.setCredentials(AuthScope.ANY,
                        new UsernamePasswordCredentials(userName, password));
                builder.setHttpClientConfigCallback((httpAsyncClientBuilder) -> {
                    httpAsyncClientBuilder.disableAuthCaching();
                    RequestConfig requestConfig = RequestConfig.custom().setConnectTimeout(120 * 1000).build();
                    return httpAsyncClientBuilder.setDefaultCredentialsProvider(provider)
                            .setMaxConnTotal(context.getMaxConnect())
                            .setMaxConnPerRoute(context.getMaxConnect())
                            .setDefaultRequestConfig(requestConfig);
                });
                esClient = EsSinkFactory.createRestHighLevelClient(builder);
            }
        } catch (Exception e) {
            LOG.error("init esclient failed.", e);
            esClient = null;
            return false;
        }
        return true;
    }

    /**
     * initBulkprocessIfNeed
     * 
     * @return
     */
    private boolean initBulkprocessIfNeed() {
        try {
            if (bulkProcessor == null) {
                EsCallbackListener esListener = new EsCallbackListener(context);
                BiConsumer<BulkRequest, ActionListener<BulkResponse>> consumer = (request, bulkListener) -> esClient
                        .bulkAsync(request, RequestOptions.DEFAULT, bulkListener);
                BulkProcessor bulkProcessor = BulkProcessor.builder(consumer, esListener)
                        .setBulkActions(context.getBulkAction())
                        .setBulkSize(new ByteSizeValue(context.getBulkSizeMb(), ByteSizeUnit.MB))
                        .setFlushInterval(TimeValue.timeValueSeconds(context.getFlushInterval()))
                        .setConcurrentRequests(context.getConcurrentRequests())
                        .build();
                this.bulkProcessor = bulkProcessor;
            }
        } catch (Exception e) {
            LOG.error("init esclient failed", e);
            esClient = null;
            return false;
        }
        return true;
    }

    /**
     *
     * close
     */
    public void close() {
        status = LifecycleState.STOP;
        try {
            this.bulkProcessor.close();
        } catch (Exception e) {
            LOG.error(String.format("close bulkProcessor:%s", e.getMessage()), e);
        }
        try {
            this.esClient.close();
        } catch (IOException e) {
            LOG.error(String.format("close EsClient:%s", e.getMessage()), e);
        }
    }

    /**
     * run
     */
    @Override
    public void run() {
        status = LifecycleState.START;
        LOG.info("start to EsOutputChannel:{},status:{}", context.getTaskName(), status);
        while (status == LifecycleState.START) {
            try {
                this.send();
            } catch (Throwable t) {
                LOG.error(t.getMessage(), t);
            }
        }
    }

    /**
     * send
     * 
     * @throws InterruptedException
     */
    public void send() throws InterruptedException {
        EsIndexRequest indexRequest = null;
        try {
            BulkProcessor bulkProcessor = this.getBulkProcessor();
            // check bulkProcessor
            if (bulkProcessor == null) {
                Thread.sleep(context.getProcessInterval());
                return;
            }
            // get indexRequest
            indexRequest = context.taskDispatchQueue();
            if (indexRequest == null) {
                Thread.sleep(context.getProcessInterval());
                return;
            }
            // send
            bulkProcessor.add(indexRequest);
            context.addSendMetric(indexRequest.getEvent(), context.getTaskName());
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
            if (indexRequest != null) {
                context.backDispatchQueue(indexRequest);
                context.addSendResultMetric(indexRequest.getEvent(), context.getTaskName(), false,
                        indexRequest.getSendTime());
            }
            try {
                Thread.sleep(context.getProcessInterval());
            } catch (InterruptedException e1) {
                LOG.error(e1.getMessage(), e1);
            }
        }
    }
}
