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

package org.apache.inlong.sort.standalone.sink.cls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tencentcloudapi.cls.producer.AsyncProducerClient;
import com.tencentcloudapi.cls.producer.AsyncProducerConfig;
import com.tencentcloudapi.cls.producer.errors.ProducerException;
import com.tencentcloudapi.cls.producer.util.NetworkUtils;

import org.apache.commons.lang3.ClassUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigHolder;
import org.apache.inlong.sort.standalone.config.pojo.InlongId;
import org.apache.inlong.sort.standalone.metrics.SortMetricItem;
import org.apache.inlong.sort.standalone.metrics.audit.AuditUtils;
import org.apache.inlong.sort.standalone.sink.SinkContext;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Cls sink context.
 */
public class ClsSinkContext extends SinkContext {

    private static final Logger LOG = InlongLoggerFactory.getLogger(ClsSinkContext.class);
    // key of sink params
    private static final String KEY_TOTAL_SIZE_IN_BYTES = "totalSizeInBytes";
    private static final String KEY_MAX_SEND_THREAD_COUNT = "maxSendThreadCount";
    private static final String KEY_MAX_BLOCK_SEC = "maxBlockSec";
    private static final String KEY_MAX_BATCH_SIZE = "maxBatchSize";
    private static final String KEY_MAX_BATCH_COUNT = "maxBatchCount";
    private static final String KEY_LINGER_MS = "lingerMs";
    private static final String KEY_RETRIES = "retries";
    private static final String KEY_MAX_RESERVED_ATTEMPTS = "maxReservedAttempts";
    private static final String KEY_BASE_RETRY_BACKOFF_MS = "baseRetryBackoffMs";
    private static final String KEY_MAX_RETRY_BACKOFF_MS = "maxRetryBackoffMs";
    private static final String KEY_MAX_KEYWORD_LENGTH = "maxKeywordLength";
    private static final String KEY_EVENT_LOG_ITEM_HANDLER = "logItemHandler";
    public static final String KEY_TOPIC_ID = "topicId";

    private static final int DEFAULT_KEYWORD_MAX_LENGTH = 32 * 1024 - 1;
    private int keywordMaxLength = DEFAULT_KEYWORD_MAX_LENGTH;

    private final Map<String, AsyncProducerClient> clientMap;
    private List<AsyncProducerClient> deletingClients = new ArrayList<>();
    private Context sinkContext;
    private Map<String, ClsIdConfig> idConfigMap = new ConcurrentHashMap<>();
    private IEvent2LogItemHandler event2LogItemHandler;

    /**
     * Constructor
     *
     * @param sinkName Name of sink.
     * @param context  Basic context.
     * @param channel  Channel which worker acquire profile event from.
     */
    public ClsSinkContext(String sinkName, Context context, Channel channel) {
        super(sinkName, context, channel);
        this.clientMap = new ConcurrentHashMap<>();
    }

    @Override
    public void reload() {
        try {
            // remove deleting clients.
            deletingClients.forEach(client -> {
                try {
                    client.close();
                } catch (InterruptedException e) {
                    LOG.error("close client failed, got InterruptedException" + e.getMessage(), e);
                } catch (ProducerException e) {
                    LOG.error("close client failed, got ProducerException" + e.getMessage(), e);
                }
            });

            SortTaskConfig newSortTaskConfig = SortClusterConfigHolder.getTaskConfig(taskName);
            if (newSortTaskConfig == null || newSortTaskConfig.equals(sortTaskConfig)) {
                return;
            }
            LOG.info("get new SortTaskConfig:taskName:{}:config:{}", taskName,
                    new ObjectMapper().writeValueAsString(newSortTaskConfig));
            this.sortTaskConfig = newSortTaskConfig;
            this.sinkContext = new Context(this.sortTaskConfig.getSinkParams());
            this.reloadIdParams();
            this.reloadClients();
            this.reloadHandler();
            this.keywordMaxLength = sinkContext.getInteger(KEY_MAX_KEYWORD_LENGTH, DEFAULT_KEYWORD_MAX_LENGTH);
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * Reload LogItemHandler.
     */
    private void reloadHandler() {
        String logItemHandlerClass = CommonPropertiesHolder.getString(KEY_EVENT_LOG_ITEM_HANDLER,
                DefaultEvent2LogItemHandler.class.getName());
        try {
            Class<?> handlerClass = ClassUtils.getClass(logItemHandlerClass);
            Object handlerObject = handlerClass.getDeclaredConstructor().newInstance();
            if (handlerObject instanceof IEvent2LogItemHandler) {
                this.event2LogItemHandler = (IEvent2LogItemHandler) handlerObject;
            } else {
                LOG.error("{} is not the instance of IEvent2LogItemHandler", logItemHandlerClass);
            }
        } catch (Throwable t) {
            LOG.error("Fail to init IEvent2LogItemHandler, handlerClass:{}, error:{}",
                    logItemHandlerClass, t.getMessage());
        }
    }

    /**
     * Reload id params.
     *
     * @throws JsonProcessingException
     */
    private void reloadIdParams() throws JsonProcessingException {
        List<Map<String, String>> idList = this.sortTaskConfig.getIdParams();
        Map<String, ClsIdConfig> newIdConfigMap = new ConcurrentHashMap<>();
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        for (Map<String, String> idParam : idList) {
            String inlongGroupId = idParam.get(Constants.INLONG_GROUP_ID);
            String inlongStreamId = idParam.get(Constants.INLONG_STREAM_ID);
            String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
            String jsonIdConfig = objectMapper.writeValueAsString(idParam);
            ClsIdConfig idConfig = objectMapper.readValue(jsonIdConfig, ClsIdConfig.class);
            idConfig.getFieldList();
            newIdConfigMap.put(uid, idConfig);
        }
        this.idConfigMap = newIdConfigMap;
    }

    /**
     * Close expire clients and start new clients.
     *
     * <p>
     * Each client response for data of one secretId.
     * </p>
     * <p>
     * First, find all secretId that are in the active clientMap but not in the updated id config (or to say EXPIRE
     * secretId), and put those clients into deletingClientsMap. The real close process will be done at the beginning of
     * next period of reloading. Second, find all secretIds that in the updated id config but not in the active
     * clientMap(or to say NEW secretId), and start new clients for these secretId and put them into the active
     * clientMap.
     * </p>
     */
    private void reloadClients() {
        // get update secretIds
        Map<String, ClsIdConfig> updateConfigMap = idConfigMap.values()
                .stream()
                .collect(Collectors.toMap(ClsIdConfig::getSecretId, config -> config, (k1, k2) -> k1));

        // remove expire client
        clientMap.keySet()
                .stream()
                .filter(secretId -> !updateConfigMap.containsKey(secretId))
                .forEach(this::removeExpireClient);

        // start new client
        updateConfigMap.values()
                .stream()
                .filter(config -> !clientMap.containsKey(config.getSecretId()))
                .forEach(this::startNewClient);
    }

    /**
     * Start new cls client and put it to the active clientMap.
     *
     * @param idConfig idConfig of new client.
     */
    private void startNewClient(ClsIdConfig idConfig) {
        AsyncProducerConfig producerConfig = new AsyncProducerConfig(
                idConfig.getEndpoint(),
                idConfig.getSecretId(),
                idConfig.getSecretKey(),
                NetworkUtils.getLocalMachineIP());
        this.setCommonClientConfig(producerConfig);
        AsyncProducerClient client = new AsyncProducerClient(producerConfig);
        clientMap.put(idConfig.getSecretId(), client);
    }

    /**
     * Get common client config from context and set them.
     *
     * @param config Config to be set.
     */
    private void setCommonClientConfig(AsyncProducerConfig config) {
        Optional.ofNullable(sinkContext.getInteger(KEY_TOTAL_SIZE_IN_BYTES))
                .ifPresent(config::setTotalSizeInBytes);
        Optional.ofNullable(sinkContext.getInteger(KEY_MAX_SEND_THREAD_COUNT))
                .ifPresent(config::setSendThreadCount);
        Optional.ofNullable(sinkContext.getInteger(KEY_MAX_BLOCK_SEC))
                .ifPresent(config::setMaxBlockMs);
        Optional.ofNullable(sinkContext.getInteger(KEY_MAX_BATCH_SIZE))
                .ifPresent(config::setBatchSizeThresholdInBytes);
        Optional.ofNullable(sinkContext.getInteger(KEY_MAX_BATCH_COUNT))
                .ifPresent(config::setBatchCountThreshold);
        Optional.ofNullable(sinkContext.getInteger(KEY_LINGER_MS))
                .ifPresent(config::setLingerMs);
        Optional.ofNullable(sinkContext.getInteger(KEY_RETRIES))
                .ifPresent(config::setRetries);
        Optional.ofNullable(sinkContext.getInteger(KEY_MAX_RESERVED_ATTEMPTS))
                .ifPresent(config::setMaxReservedAttempts);
        Optional.ofNullable(sinkContext.getInteger(KEY_BASE_RETRY_BACKOFF_MS))
                .ifPresent(config::setBaseRetryBackoffMs);
        Optional.ofNullable(sinkContext.getInteger(KEY_MAX_RETRY_BACKOFF_MS))
                .ifPresent(config::setMaxRetryBackoffMs);
    }

    /**
     * Remove expire client from active clientMap and into the deleting client list.
     * <P>
     * The reason why not close client when it remove from clientMap is to avoid <b>Race Condition</b>. Which will
     * happen when worker thread get the client and ready to send msg, while the reload thread try to close it.
     * </P>
     *
     * @param secretId SecretId of expire client.
     */
    private void removeExpireClient(String secretId) {
        AsyncProducerClient client = clientMap.get(secretId);
        if (client == null) {
            LOG.error("Remove client failed, there is not client of {}", secretId);
            return;
        }
        deletingClients.add(clientMap.remove(secretId));
    }

    /**
     * Add send result.
     *
     * @param currentRecord Event to be sent.
     * @param bid           Topic or dest ip of event.
     * @param result        Result of send.
     * @param sendTime      Time of sending.
     */
    public void addSendResultMetric(ProfileEvent currentRecord, String bid, boolean result, long sendTime) {
        Map<String, String> dimensions = this.getDimensions(currentRecord, bid);
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        if (result) {
            metricItem.sendSuccessCount.incrementAndGet();
            metricItem.sendSuccessSize.addAndGet(currentRecord.getBody().length);
            AuditUtils.add(AuditUtils.AUDIT_ID_SEND_SUCCESS, currentRecord);
            if (sendTime > 0) {
                final long currentTime = System.currentTimeMillis();
                long sinkDuration = currentTime - sendTime;
                long nodeDuration = currentTime - currentRecord.getFetchTime();
                long wholeDuration = currentTime - currentRecord.getRawLogTime();
                metricItem.sinkDuration.addAndGet(sinkDuration);
                metricItem.nodeDuration.addAndGet(nodeDuration);
                metricItem.wholeDuration.addAndGet(wholeDuration);
            }
        } else {
            metricItem.sendFailCount.incrementAndGet();
            metricItem.sendFailSize.addAndGet(currentRecord.getBody().length);
        }
    }

    /**
     * Get report dimensions.
     *
     * @param  currentRecord Event.
     * @param  bid  Topic or dest ip.
     * @return  Prepared dimensions map.
     */
    private Map<String, String> getDimensions(ProfileEvent currentRecord, String bid) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        // metric
        fillInlongId(currentRecord, dimensions);
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, bid);
        long msgTime = currentRecord.getRawLogTime();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        return dimensions;
    }

    /**
     * Get {@link ClsIdConfig} by uid.
     *
     * @param  uid Uid of event.
     * @return  Corresponding cls id config.
     */
    public ClsIdConfig getIdConfig(String uid) {
        return idConfigMap.get(uid);
    }

    /**
     * Get max length of single value.
     *
     * @return Max length of single value.
     */
    public int getKeywordMaxLength() {
        return keywordMaxLength;
    }

    /**
     * Get LogItem handler.
     *
     * @return Handler.
     */
    public IEvent2LogItemHandler getLogItemHandler() {
        return event2LogItemHandler;
    }

    /**
     * Get cls client.
     *
     * @param  secretId ID of client.
     * @return  Client instance.
     */
    public AsyncProducerClient getClient(String secretId) {
        return clientMap.get(secretId);
    }
}
