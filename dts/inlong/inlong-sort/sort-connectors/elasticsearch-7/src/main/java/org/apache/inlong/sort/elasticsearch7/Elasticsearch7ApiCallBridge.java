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

package org.apache.inlong.sort.elasticsearch7;

import org.apache.flink.annotation.Internal;
import org.apache.flink.streaming.connectors.elasticsearch7.RestClientFactory;
import org.apache.flink.util.Preconditions;
import org.apache.http.HttpHost;
import org.apache.inlong.sort.elasticsearch.BulkProcessorOptions;
import org.apache.inlong.sort.elasticsearch.ElasticsearchApiCallBridge;
import org.apache.inlong.sort.elasticsearch.ElasticsearchSinkBase;
import org.apache.inlong.sort.elasticsearch.RequestIndexer;
import org.elasticsearch.action.DocWriteRequest;
import org.elasticsearch.action.bulk.BackoffPolicy;
import org.elasticsearch.action.bulk.BulkItemResponse;
import org.elasticsearch.action.bulk.BulkProcessor;
import org.elasticsearch.action.bulk.BulkProcessor.Builder;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.ByteSizeUnit;
import org.elasticsearch.common.unit.ByteSizeValue;
import org.elasticsearch.common.unit.TimeValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link ElasticsearchApiCallBridge} for Elasticsearch 7 and later versions.
 */
@Internal
public class Elasticsearch7ApiCallBridge
        implements
            ElasticsearchApiCallBridge<DocWriteRequest<?>, BulkProcessor.Builder, BulkProcessor.Listener, BulkItemResponse, BulkProcessor, RestHighLevelClient> {

    private static final long serialVersionUID = -5222683870097809633L;

    private static final Logger LOGGER = LoggerFactory.getLogger(Elasticsearch7ApiCallBridge.class);

    /**
     * User-provided HTTP Host.
     */
    private final List<HttpHost> httpHosts;

    /**
     * The factory to configure the rest client.
     */
    private final RestClientFactory restClientFactory;

    Elasticsearch7ApiCallBridge(List<HttpHost> httpHosts, RestClientFactory restClientFactory) {
        Preconditions.checkArgument(httpHosts != null && !httpHosts.isEmpty());
        this.httpHosts = httpHosts;
        this.restClientFactory = Preconditions.checkNotNull(restClientFactory);
    }

    @Override
    public RestHighLevelClient createClient(Map<String, String> clientConfig) {
        RestClientBuilder builder =
                RestClient.builder(httpHosts.toArray(new HttpHost[0]));
        restClientFactory.configureRestClientBuilder(builder);
        return new RestHighLevelClient(builder);
    }

    @Override
    public BulkProcessor.Builder createBulkProcessorBuilder(
            RestHighLevelClient client, BulkProcessor.Listener listener) {
        return BulkProcessor.builder(
                (request, bulkListener) -> client
                        .bulkAsync(request, RequestOptions.DEFAULT, bulkListener),
                listener);
    }

    @Override
    public BulkProcessor buildBulkProcessor(Builder builder) {
        return builder.build();
    }

    @Override
    public Throwable extractFailureCauseFromBulkItemResponse(BulkItemResponse bulkItemResponse) {
        if (!bulkItemResponse.isFailed()) {
            return null;
        } else {
            return bulkItemResponse.getFailure().getCause();
        }
    }

    @Override
    public void configureBulkProcessorBackoff(BulkProcessor.Builder builder,
            @Nullable ElasticsearchSinkBase.BulkFlushBackoffPolicy flushBackoffPolicy) {
        BackoffPolicy backoffPolicy;
        if (flushBackoffPolicy != null) {
            switch (flushBackoffPolicy.getBackoffType()) {
                case CONSTANT:
                    backoffPolicy =
                            BackoffPolicy.constantBackoff(
                                    new TimeValue(flushBackoffPolicy.getDelayMillis()),
                                    flushBackoffPolicy.getMaxRetryCount());
                    break;
                case EXPONENTIAL:
                default:
                    backoffPolicy =
                            BackoffPolicy.exponentialBackoff(
                                    new TimeValue(flushBackoffPolicy.getDelayMillis()),
                                    flushBackoffPolicy.getMaxRetryCount());
            }
        } else {
            backoffPolicy = BackoffPolicy.noBackoff();
        }

        builder.setBackoffPolicy(backoffPolicy);
    }

    @Override
    public RequestIndexer<DocWriteRequest<?>> createBulkProcessorIndexer(
            BulkProcessor bulkProcessor,
            boolean flushOnCheckpoint,
            AtomicLong numPendingRequestsRef) {
        return new Elasticsearch7BulkProcessorIndexer(
                bulkProcessor, flushOnCheckpoint, numPendingRequestsRef);
    }

    @Override
    public void configureBulkProcessor(Builder builder, BulkProcessorOptions options) {
        builder.setConcurrentRequests(options.getConcurrentRequests());
        if (options.getFlushMaxActions() != null) {
            builder.setBulkActions(options.getFlushMaxActions());
        }
        if (options.getFlushMaxSizeMb() != null) {
            final ByteSizeUnit sizeUnit;
            if (options.getFlushMaxSizeMb() == -1) {
                // bulk size can be disabled with -1, however the ByteSizeValue constructor accepts -1
                // only with BYTES as the size unit
                sizeUnit = ByteSizeUnit.BYTES;
            } else {
                sizeUnit = ByteSizeUnit.MB;
            }
            builder.setBulkSize(new ByteSizeValue(options.getFlushMaxSizeMb(), sizeUnit));
        }
        if (options.getFlushInterval() != null) {
            if (options.getFlushInterval() == -1) {
                builder.setFlushInterval(null);
            } else {
                builder.setFlushInterval(TimeValue.timeValueMillis(options.getFlushInterval()));
            }
        }
    }

    @Override
    public void verifyClientConnection(RestHighLevelClient client) throws IOException {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Pinging Elasticsearch cluster via hosts {} ...", httpHosts);
        }

        if (!client.ping(RequestOptions.DEFAULT)) {
            throw new RuntimeException("There are no reachable Elasticsearch nodes!");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Elasticsearch RestHighLevelClient is connected to {}", httpHosts.toString());
        }
    }
}
