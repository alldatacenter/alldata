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

package org.apache.inlong.sort.standalone.sink.elasticsearch;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.http.HttpHost;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigHolder;
import org.apache.inlong.sort.standalone.config.pojo.InlongId;
import org.apache.inlong.sort.standalone.metrics.SortMetricItem;
import org.apache.inlong.sort.standalone.metrics.audit.AuditUtils;
import org.apache.inlong.sort.standalone.sink.SinkContext;
import org.apache.inlong.sort.standalone.utils.BufferQueue;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 
 * EsSinkContext
 */
public class EsSinkContext extends SinkContext {

    public static final Logger LOG = InlongLoggerFactory.getLogger(EsSinkContext.class);
    public static final String KEY_NODE_ID = "nodeId";
    public static final String KEY_USERNAME = "username";
    public static final String KEY_PASSWORD = "password";
    public static final String KEY_BULK_ACTION = "bulkAction";
    public static final String KEY_BULK_SIZE_MB = "bulkSizeMb";
    public static final String KEY_FLUSH_INTERVAL = "flushInterval";
    public static final String KEY_CONCURRENT_REQUESTS = "concurrentRequests";
    public static final String KEY_MAX_CONNECT_TOTAL = "maxConnect";
    public static final String KEY_MAX_CONNECT_PER_ROUTE = "maxConnectPerRoute";
    public static final String KEY_CONNECTION_REQUEST_TIMEOUT = "connectionRequestTimeout";
    public static final String KEY_SOCKET_TIMEOUT = "socketTimeout";
    public static final String KEY_MAX_REDIRECTS = "maxRedirects";
    public static final String KEY_LOG_MAX_LENGTH = "logMaxLength";
    public static final String KEY_KEYWORD_MAX_LENGTH = "keywordMaxLength";
    public static final String KEY_HTTP_HOSTS = "httpHosts";
    public static final String KEY_EVENT_INDEXREQUEST_HANDLER = "indexRequestHandler";
    public static final String KEY_IS_USE_INDEX_ID = "isUseIndexId";

    public static final int DEFAULT_BULK_ACTION = 10000;
    public static final int DEFAULT_BULK_SIZE_MB = 20;
    public static final int DEFAULT_FLUSH_INTERVAL = 2000;
    public static final int DEFAULT_CONCURRENT_REQUESTS = 20;
    public static final int DEFAULT_MAX_CONNECT_TOTAL = 1000;
    public static final int DEFAULT_MAX_CONNECT_PER_ROUTE = 1000;
    public static final int DEFAULT_CONNECTION_REQUEST_TIMEOUT = 0;
    public static final int DEFAULT_SOCKET_TIMEOUT = 0;
    public static final int DEFAULT_MAX_REDIRECTS = 0;
    public static final int DEFAULT_LOG_MAX_LENGTH = 32 * 1024;
    public static final int DEFAULT_KEYWORD_MAX_LENGTH = 8 * 1024;
    public static final boolean DEFAULT_IS_USE_INDEX_ID = false;

    private Context sinkContext;
    private String nodeId;
    private Map<String, EsIdConfig> idConfigMap = new ConcurrentHashMap<>();
    private final BufferQueue<EsIndexRequest> dispatchQueue;
    private AtomicLong offerCounter = new AtomicLong(0);
    private AtomicLong takeCounter = new AtomicLong(0);
    private AtomicLong backCounter = new AtomicLong(0);
    // rest client
    private String username;
    private String password;
    private int bulkAction = DEFAULT_BULK_ACTION;
    private int bulkSizeMb = DEFAULT_BULK_SIZE_MB;
    private int flushInterval = DEFAULT_FLUSH_INTERVAL;
    private int concurrentRequests = DEFAULT_CONCURRENT_REQUESTS;
    private int maxConnect = DEFAULT_MAX_CONNECT_TOTAL;
    private int maxConnectPerRoute = DEFAULT_MAX_CONNECT_PER_ROUTE;
    private int connectionRequestTimeout = DEFAULT_CONNECTION_REQUEST_TIMEOUT;
    private int socketTimeout = DEFAULT_SOCKET_TIMEOUT;
    private int maxRedirects = DEFAULT_MAX_REDIRECTS;
    private int logMaxLength = DEFAULT_LOG_MAX_LENGTH;
    private int keywordMaxLength = DEFAULT_KEYWORD_MAX_LENGTH;
    private boolean isUseIndexId = DEFAULT_IS_USE_INDEX_ID;
    // http host
    private String strHttpHosts;
    private HttpHost[] httpHosts;

    /**
     * Constructor
     * 
     * @param sinkName
     * @param context
     * @param channel
     * @param dispatchQueue
     */
    public EsSinkContext(String sinkName, Context context, Channel channel,
            BufferQueue<EsIndexRequest> dispatchQueue) {
        super(sinkName, context, channel);
        this.sinkContext = context;
        this.dispatchQueue = dispatchQueue;
        this.nodeId = CommonPropertiesHolder.getString(KEY_NODE_ID);
    }

    /**
     * reload
     */
    public void reload() {
        try {
            LOG.info("SortTask:{},dispatchQueue:{},offer:{},take:{},back:{}",
                    taskName, dispatchQueue.size(), offerCounter.getAndSet(0),
                    takeCounter.getAndSet(0), backCounter.getAndSet(0));
            SortTaskConfig newSortTaskConfig = SortClusterConfigHolder.getTaskConfig(taskName);
            if (this.sortTaskConfig != null && this.sortTaskConfig.equals(newSortTaskConfig)) {
                return;
            }
            LOG.info("get new SortTaskConfig:taskName:{}:config:{}", taskName,
                    new ObjectMapper().writeValueAsString(newSortTaskConfig));
            this.sortTaskConfig = newSortTaskConfig;
            this.sinkContext = new Context(this.sortTaskConfig.getSinkParams());
            // parse the config of id and topic
            Map<String, EsIdConfig> newIdConfigMap = new ConcurrentHashMap<>();
            List<Map<String, String>> idList = this.sortTaskConfig.getIdParams();
            ObjectMapper objectMapper = new ObjectMapper();
            for (Map<String, String> idParam : idList) {
                String inlongGroupId = idParam.get(Constants.INLONG_GROUP_ID);
                String inlongStreamId = idParam.get(Constants.INLONG_STREAM_ID);
                String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
                String jsonIdConfig = objectMapper.writeValueAsString(idParam);
                EsIdConfig idConfig = objectMapper.readValue(jsonIdConfig, EsIdConfig.class);
                idConfig.getFieldList();
                newIdConfigMap.put(uid, idConfig);
            }
            // change current config
            this.idConfigMap = newIdConfigMap;
            // rest client
            this.username = sinkContext.getString(KEY_USERNAME);
            this.password = sinkContext.getString(KEY_PASSWORD);
            this.bulkAction = sinkContext.getInteger(KEY_BULK_ACTION, DEFAULT_BULK_ACTION);
            this.bulkSizeMb = sinkContext.getInteger(KEY_BULK_SIZE_MB, DEFAULT_BULK_SIZE_MB);
            this.flushInterval = sinkContext.getInteger(KEY_FLUSH_INTERVAL, DEFAULT_FLUSH_INTERVAL);
            this.concurrentRequests = sinkContext.getInteger(KEY_CONCURRENT_REQUESTS, DEFAULT_CONCURRENT_REQUESTS);
            this.maxConnect = sinkContext.getInteger(KEY_MAX_CONNECT_TOTAL, DEFAULT_MAX_CONNECT_TOTAL);
            this.maxConnectPerRoute = sinkContext.getInteger(KEY_MAX_CONNECT_PER_ROUTE, DEFAULT_MAX_CONNECT_PER_ROUTE);
            this.connectionRequestTimeout =
                    sinkContext.getInteger(KEY_CONNECTION_REQUEST_TIMEOUT, DEFAULT_CONNECTION_REQUEST_TIMEOUT);
            this.socketTimeout = sinkContext.getInteger(KEY_SOCKET_TIMEOUT, DEFAULT_SOCKET_TIMEOUT);
            this.maxRedirects = sinkContext.getInteger(KEY_MAX_REDIRECTS, DEFAULT_MAX_REDIRECTS);
            this.logMaxLength = sinkContext.getInteger(KEY_LOG_MAX_LENGTH, DEFAULT_LOG_MAX_LENGTH);
            this.keywordMaxLength = sinkContext.getInteger(KEY_KEYWORD_MAX_LENGTH, DEFAULT_KEYWORD_MAX_LENGTH);
            this.isUseIndexId = sinkContext.getBoolean(KEY_IS_USE_INDEX_ID, DEFAULT_IS_USE_INDEX_ID);
            // http host
            this.strHttpHosts = sinkContext.getString(KEY_HTTP_HOSTS);
            if (!StringUtils.isBlank(strHttpHosts)) {
                String[] strHttpHostArray = strHttpHosts.split("\\s+");
                List<HttpHost> newHttpHosts = new ArrayList<>(strHttpHostArray.length);
                for (String strHttpHost : strHttpHostArray) {
                    String[] ipPort = strHttpHost.split(":");
                    if (ipPort.length == 2 && NumberUtils.isDigits(ipPort[1])) {
                        newHttpHosts.add(new HttpHost(ipPort[0], NumberUtils.toInt(ipPort[1])));
                    }
                }
                if (newHttpHosts.size() > 0) {
                    HttpHost[] newHostHostArray = new HttpHost[newHttpHosts.size()];
                    this.httpHosts = newHttpHosts.toArray(newHostHostArray);
                }
            }
            // log
            LOG.info("end to get SortTaskConfig:taskName:{}:newIdConfigMap:{}", taskName,
                    new ObjectMapper().writeValueAsString(newIdConfigMap));
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * addSendMetric
     * 
     * @param currentRecord
     * @param bid
     */
    public void addSendMetric(ProfileEvent currentRecord, String bid) {
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
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        long count = 1;
        long size = currentRecord.getBody().length;
        metricItem.sendCount.addAndGet(count);
        metricItem.sendSize.addAndGet(size);
    }

    /**
     * addReadFailMetric
     */
    public void addSendFailMetric() {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        long msgTime = System.currentTimeMillis();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        metricItem.readFailCount.incrementAndGet();
    }

    /**
     * addSendResultMetric
     * 
     * @param currentRecord
     * @param bid
     * @param result
     * @param sendTime
     */
    public void addSendResultMetric(ProfileEvent currentRecord, String bid, boolean result, long sendTime) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        // metric
        fillInlongId(currentRecord, dimensions);
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, bid);
        final long currentTime = System.currentTimeMillis();
        long msgTime = currentRecord.getRawLogTime();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        if (result) {
            metricItem.sendSuccessCount.incrementAndGet();
            metricItem.sendSuccessSize.addAndGet(currentRecord.getBody().length);
            AuditUtils.add(AuditUtils.AUDIT_ID_SEND_SUCCESS, currentRecord);
            if (sendTime > 0) {
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
     * getIdConfig
     * 
     * @param  uid
     * @return
     */
    public EsIdConfig getIdConfig(String uid) {
        return this.idConfigMap.get(uid);
    }

    /**
     * get nodeId
     * 
     * @return the nodeId
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * get idConfigMap
     * 
     * @return the idConfigMap
     */
    public Map<String, EsIdConfig> getIdConfigMap() {
        return idConfigMap;
    }

    /**
     * get sinkContext
     * 
     * @return the sinkContext
     */
    public Context getSinkContext() {
        return sinkContext;
    }

    /**
     * set sinkContext
     * 
     * @param sinkContext the sinkContext to set
     */
    public void setSinkContext(Context sinkContext) {
        this.sinkContext = sinkContext;
    }

    /**
     * offerDispatchQueue
     * 
     * @param  indexRequest
     * @return
     */
    public void offerDispatchQueue(EsIndexRequest indexRequest) {
        this.offerCounter.incrementAndGet();
        dispatchQueue.acquire(indexRequest.getEvent().getBody().length);
        dispatchQueue.offer(indexRequest);
    }

    /**
     * takeDispatchQueue
     * 
     * @return
     */
    public EsIndexRequest takeDispatchQueue() {
        EsIndexRequest indexRequest = this.dispatchQueue.pollRecord();
        if (indexRequest != null) {
            this.takeCounter.incrementAndGet();
        }
        return indexRequest;
    }

    /**
     * backDispatchQueue
     * 
     * @param  indexRequest
     * @return
     */
    public void backDispatchQueue(EsIndexRequest indexRequest) {
        this.backCounter.incrementAndGet();
        dispatchQueue.offer(indexRequest);
    }

    /**
     * releaseDispatchQueue
     * 
     * @param  indexRequest
     * @return
     */
    public void releaseDispatchQueue(EsIndexRequest indexRequest) {
        dispatchQueue.release(indexRequest.getEvent().getBody().length);
    }

    /**
     * get bulkAction
     * 
     * @return the bulkAction
     */
    public int getBulkAction() {
        return bulkAction;
    }

    /**
     * set bulkAction
     * 
     * @param bulkAction the bulkAction to set
     */
    public void setBulkAction(int bulkAction) {
        this.bulkAction = bulkAction;
    }

    /**
     * get bulkSizeMb
     * 
     * @return the bulkSizeMb
     */
    public int getBulkSizeMb() {
        return bulkSizeMb;
    }

    /**
     * set bulkSizeMb
     * 
     * @param bulkSizeMb the bulkSizeMb to set
     */
    public void setBulkSizeMb(int bulkSizeMb) {
        this.bulkSizeMb = bulkSizeMb;
    }

    /**
     * get flushInterval
     * 
     * @return the flushInterval
     */
    public int getFlushInterval() {
        return flushInterval;
    }

    /**
     * set flushInterval
     * 
     * @param flushInterval the flushInterval to set
     */
    public void setFlushInterval(int flushInterval) {
        this.flushInterval = flushInterval;
    }

    /**
     * get concurrentRequests
     * 
     * @return the concurrentRequests
     */
    public int getConcurrentRequests() {
        return concurrentRequests;
    }

    /**
     * set concurrentRequests
     * 
     * @param concurrentRequests the concurrentRequests to set
     */
    public void setConcurrentRequests(int concurrentRequests) {
        this.concurrentRequests = concurrentRequests;
    }

    /**
     * get maxConnect
     * 
     * @return the maxConnect
     */
    public int getMaxConnect() {
        return maxConnect;
    }

    /**
     * get MaxConnectPerRoute
     */
    public int getMaxConnectPerRoute() {
        return maxConnectPerRoute;
    }

    /**
     * get ConnectionRequestTimeout
     */
    public int getConnectionRequestTimeout() {
        return connectionRequestTimeout;
    }

    /**
     * get SocketTimeout
     */
    public int getSocketTimeout() {
        return socketTimeout;
    }

    /**
     * get MaxRedirects
     */
    public int getMaxRedirects() {
        return maxRedirects;
    }

    /**
     * get LogMaxLength
     */
    public int getLogMaxLength() {
        return logMaxLength;
    }

    /**
     * set maxConnect
     * 
     * @param maxConnect the maxConnect to set
     */
    public void setMaxConnect(int maxConnect) {
        this.maxConnect = maxConnect;
    }

    /**
     * get strHttpHosts
     * 
     * @return the strHttpHosts
     */
    public String getStrHttpHosts() {
        return strHttpHosts;
    }

    /**
     * set strHttpHosts
     * 
     * @param strHttpHosts the strHttpHosts to set
     */
    public void setStrHttpHosts(String strHttpHosts) {
        this.strHttpHosts = strHttpHosts;
    }

    /**
     * get httpHosts
     * 
     * @return the httpHosts
     */
    public HttpHost[] getHttpHosts() {
        return httpHosts;
    }

    /**
     * set httpHosts
     * 
     * @param httpHosts the httpHosts to set
     */
    public void setHttpHosts(HttpHost[] httpHosts) {
        this.httpHosts = httpHosts;
    }

    /**
     * set nodeId
     * 
     * @param nodeId the nodeId to set
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * set idConfigMap
     * 
     * @param idConfigMap the idConfigMap to set
     */
    public void setIdConfigMap(Map<String, EsIdConfig> idConfigMap) {
        this.idConfigMap = idConfigMap;
    }

    /**
     * get username
     * 
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * set username
     * 
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * get password
     * 
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * set password
     * 
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * get keywordMaxLength
     * 
     * @return the keywordMaxLength
     */
    public int getKeywordMaxLength() {
        return keywordMaxLength;
    }

    /**
     * set keywordMaxLength
     * 
     * @param keywordMaxLength the keywordMaxLength to set
     */
    public void setKeywordMaxLength(int keywordMaxLength) {
        this.keywordMaxLength = keywordMaxLength;
    }

    /**
     * get isUseIndexId
     * 
     * @return the isUseIndexId
     */
    public boolean isUseIndexId() {
        return isUseIndexId;
    }

    /**
     * set isUseIndexId
     * 
     * @param isUseIndexId the isUseIndexId to set
     */
    public void setUseIndexId(boolean isUseIndexId) {
        this.isUseIndexId = isUseIndexId;
    }

    /**
     * create indexRequestHandler
     * 
     * @return the indexRequestHandler
     */
    public IEvent2IndexRequestHandler createIndexRequestHandler() {
        // IEvent2IndexRequestHandler
        String indexRequestHandlerClass = CommonPropertiesHolder.getString(KEY_EVENT_INDEXREQUEST_HANDLER,
                DefaultEvent2IndexRequestHandler.class.getName());
        try {
            Class<?> handlerClass = ClassUtils.getClass(indexRequestHandlerClass);
            Object handlerObject = handlerClass.getDeclaredConstructor().newInstance();
            if (handlerObject instanceof IEvent2IndexRequestHandler) {
                IEvent2IndexRequestHandler handler = (IEvent2IndexRequestHandler) handlerObject;
                return handler;
            }
        } catch (Throwable t) {
            LOG.error("Fail to init IEvent2IndexRequestHandler,handlerClass:{},error:{}",
                    indexRequestHandlerClass, t.getMessage(), t);
        }
        return null;
    }
}
