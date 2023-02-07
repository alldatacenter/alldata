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

package org.apache.inlong.sort.standalone.sink.hive;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.inlong.sort.standalone.config.holder.CommonPropertiesHolder;
import org.apache.inlong.sort.standalone.config.holder.SortClusterConfigHolder;
import org.apache.inlong.sort.standalone.config.pojo.InlongId;
import org.apache.inlong.sort.standalone.dispatch.DispatchProfile;
import org.apache.inlong.sort.standalone.metrics.SortMetricItem;
import org.apache.inlong.sort.standalone.metrics.audit.AuditUtils;
import org.apache.inlong.sort.standalone.sink.SinkContext;
import org.apache.inlong.sort.standalone.utils.Constants;
import org.apache.inlong.sort.standalone.utils.InlongLoggerFactory;
import org.slf4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * HiveSinkContext
 */
public class HiveSinkContext extends SinkContext {

    public static final Logger LOG = InlongLoggerFactory.getLogger(HiveSinkContext.class);
    public static final String KEY_NODE_ID = "nodeId";
    public static final String HIVE_DRIVER = "org.apache.hive.jdbc.HiveDriver";
    // hdfs config
    public static final String KEY_HDFS_PATH = "hdfsPath";
    public static final String KEY_MAX_FILE_OPEN_DELAY = "maxFileOpenDelayMinute";
    public static final long DEFAULT_MAX_FILE_OPEN_DELAY = 5L;
    public static final String KEY_EVENT_FORMAT_HANDLER = "eventFormatHandler";
    public static final String KEY_TOKEN_OVERTIME = "tokenOvertimeMinute";
    public static final long DEFAULT_TOKEN_OVERTIME = 60L;
    public static final String KEY_MAX_OUTPUT_FILE_SIZE = "maxOutputFileSizeGb";
    public static final long DEFAULT_MAX_OUTPUT_FILE_SIZE = 2L;
    public static final long MINUTE_MS = 60L * 1000;
    public static final long GB_BYTES = 1024L * 1024 * 1024;
    public static final long KB_BYTES = 1024L;

    // hive config
    public static final String KEY_HIVE_JDBC_URL = "hiveJdbcUrl";
    public static final String KEY_HIVE_DATABASE = "hiveDatabase";
    public static final String KEY_HIVE_USERNAME = "hiveUsername";
    public static final String KEY_HIVE_PASSWORD = "hivePassword";

    private Context parentContext;
    private String nodeId;
    private Map<String, HdfsIdConfig> idConfigMap = new ConcurrentHashMap<>();
    private LinkedBlockingQueue<DispatchProfile> dispatchQueue = new LinkedBlockingQueue<>();
    // hdfs config
    private String hdfsPath;
    private long maxFileOpenDelayMinute = DEFAULT_MAX_FILE_OPEN_DELAY;
    private long fileArchiveDelayMinute = 2 * maxFileOpenDelayMinute;
    private long tokenOvertimeMinute = DEFAULT_TOKEN_OVERTIME;
    private long maxOutputFileSizeGb = DEFAULT_MAX_OUTPUT_FILE_SIZE;
    // hive config
    private String hiveJdbcUrl;
    private String hiveDatabase;
    private String hiveUsername;
    private String hivePassword;
    // write file thread pool
    private ExecutorService outputPool;
    // scheduled thread pool
    // partition create runnable
    private ExecutorService partitionCreatePool;
    private IEventFormatHandler eventFormatHandler;

    /**
     * Constructor
     * 
     * @param sinkName
     * @param context
     * @param channel
     */
    public HiveSinkContext(String sinkName, Context context, Channel channel,
            LinkedBlockingQueue<DispatchProfile> dispatchQueue) {
        super(sinkName, context, channel);
        this.parentContext = context;
        this.dispatchQueue = dispatchQueue;
        this.nodeId = CommonPropertiesHolder.getString(KEY_NODE_ID, NetworkUtils.getLocalIp());
        this.outputPool = Executors.newFixedThreadPool(this.getMaxThreads());
        this.partitionCreatePool = Executors.newFixedThreadPool(this.getMaxThreads());
        String eventFormatHandlerClass = CommonPropertiesHolder.getString(KEY_EVENT_FORMAT_HANDLER,
                DefaultEventFormatHandler.class.getName());
        try {
            Class<?> handlerClass = ClassUtils.getClass(eventFormatHandlerClass);
            Object handlerObject = handlerClass.getDeclaredConstructor().newInstance();
            if (handlerObject instanceof IEventFormatHandler) {
                this.eventFormatHandler = (IEventFormatHandler) handlerObject;
            }
        } catch (Throwable t) {
            LOG.error("Fail to init IEventFormatHandler,handlerClass:{},error:{}",
                    eventFormatHandlerClass, t.getMessage());
        }
    }

    /**
     * reload
     */
    public void reload() {
        try {
            SortTaskConfig newSortTaskConfig = SortClusterConfigHolder.getTaskConfig(taskName);
            LOG.info("start to get SortTaskConfig:taskName:{}:config:{}", taskName,
                    new ObjectMapper().writeValueAsString(newSortTaskConfig));
            if (this.sortTaskConfig != null && this.sortTaskConfig.equals(newSortTaskConfig)) {
                return;
            }
            this.sortTaskConfig = newSortTaskConfig;
            this.parentContext = new Context(this.sortTaskConfig.getSinkParams());
            // parse the config of id and topic
            Map<String, HdfsIdConfig> newIdConfigMap = new ConcurrentHashMap<>();
            List<Map<String, String>> idList = this.sortTaskConfig.getIdParams();
            ObjectMapper objectMapper = new ObjectMapper();
            for (Map<String, String> idParam : idList) {
                String inlongGroupId = idParam.get(Constants.INLONG_GROUP_ID);
                String inlongStreamId = idParam.get(Constants.INLONG_STREAM_ID);
                String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
                String jsonIdConfig = objectMapper.writeValueAsString(idParam);
                HdfsIdConfig idConfig = objectMapper.readValue(jsonIdConfig, HdfsIdConfig.class);
                newIdConfigMap.put(uid, idConfig);
            }
            // change current config
            this.idConfigMap = newIdConfigMap;
            // hdfs config
            this.hdfsPath = parentContext.getString(KEY_HDFS_PATH);
            this.maxFileOpenDelayMinute = parentContext.getLong(KEY_MAX_FILE_OPEN_DELAY, DEFAULT_MAX_FILE_OPEN_DELAY);
            this.fileArchiveDelayMinute = maxFileOpenDelayMinute + 1;
            this.tokenOvertimeMinute = parentContext.getLong(KEY_TOKEN_OVERTIME, DEFAULT_TOKEN_OVERTIME);
            this.maxOutputFileSizeGb = parentContext.getLong(KEY_MAX_OUTPUT_FILE_SIZE, DEFAULT_MAX_OUTPUT_FILE_SIZE);
            // hive config
            this.hiveJdbcUrl = parentContext.getString(KEY_HIVE_JDBC_URL);
            this.hiveDatabase = parentContext.getString(KEY_HIVE_DATABASE);
            this.hiveUsername = parentContext.getString(KEY_HIVE_USERNAME);
            this.hivePassword = parentContext.getString(KEY_HIVE_PASSWORD);
            Class.forName("org.apache.hive.jdbc.HiveDriver");
            LOG.info("end to get SortTaskConfig:taskName:{}:newIdConfigMap:{}", taskName,
                    new ObjectMapper().writeValueAsString(newIdConfigMap));
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * addSendMetric
     * 
     * @param currentRecord
     * @param bid
     */
    public void addSendMetric(DispatchProfile currentRecord, String bid) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        // metric
        fillInlongId(currentRecord, dimensions);
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, bid);
        long msgTime = currentRecord.getDispatchTime();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        long count = currentRecord.getCount();
        long size = currentRecord.getSize();
        metricItem.sendCount.addAndGet(count);
        metricItem.sendSize.addAndGet(size);
        // LOG.info("addSendMetric,bid:{},count:{},metric:{}",
        // bid, currentRecord.getCount(), JSON.toJSONString(metricItemSet.getItemMap()));
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
     * fillInlongId
     * 
     * @param currentRecord
     * @param dimensions
     */
    public static void fillInlongId(DispatchProfile currentRecord, Map<String, String> dimensions) {
        String inlongGroupId = currentRecord.getInlongGroupId();
        inlongGroupId = (StringUtils.isBlank(inlongGroupId)) ? "-" : inlongGroupId;
        String inlongStreamId = currentRecord.getInlongStreamId();
        inlongStreamId = (StringUtils.isBlank(inlongStreamId)) ? "-" : inlongStreamId;
        dimensions.put(SortMetricItem.KEY_INLONG_GROUP_ID, inlongGroupId);
        dimensions.put(SortMetricItem.KEY_INLONG_STREAM_ID, inlongStreamId);
    }

    /**
     * addSendResultMetric
     * 
     * @param currentRecord
     * @param bid
     * @param result
     * @param sendTime
     */
    public void addSendResultMetric(DispatchProfile currentRecord, String bid, boolean result, long sendTime) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        // metric
        fillInlongId(currentRecord, dimensions);
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, bid);
        long msgTime = currentRecord.getDispatchTime();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        long count = currentRecord.getCount();
        long size = currentRecord.getSize();
        if (result) {
            metricItem.sendSuccessCount.addAndGet(count);
            metricItem.sendSuccessSize.addAndGet(size);
            currentRecord.getEvents().forEach((event) -> {
                AuditUtils.add(AuditUtils.AUDIT_ID_SEND_SUCCESS, event);
            });
            if (sendTime > 0) {
                long currentTime = System.currentTimeMillis();
                long sinkDuration = currentTime - sendTime;
                long nodeDuration = currentTime - NumberUtils.toLong(Constants.HEADER_KEY_SOURCE_TIME, msgTime);
                long wholeDuration = currentTime - msgTime;
                metricItem.sinkDuration.addAndGet(sinkDuration * count);
                metricItem.nodeDuration.addAndGet(nodeDuration * count);
                metricItem.wholeDuration.addAndGet(wholeDuration * count);
            }
            // LOG.info("addSendTrueMetric,bid:{},result:{},sendTime:{},count:{},metric:{}",
            // bid, result, sendTime, currentRecord.getCount(), JSON.toJSONString(metricItemSet.getItemMap()));
        } else {
            metricItem.sendFailCount.addAndGet(count);
            metricItem.sendFailSize.addAndGet(size);
            // LOG.info("addSendFalseMetric,bid:{},result:{},sendTime:{},count:{},metric:{}",
            // bid, result, sendTime, currentRecord.getCount(), JSON.toJSONString(metricItemSet.getItemMap()));
        }
    }

    /**
     * getHiveConnection
     * 
     * @return                        Connection
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public Connection getHiveConnection() throws SQLException, ClassNotFoundException {
        Class.forName(HIVE_DRIVER);

        String connStr = hiveJdbcUrl + "/" + hiveDatabase;
        Connection connection = DriverManager.getConnection(connStr, hiveUsername, hivePassword);

        LOG.info("Connect to hive {} successfully", connStr);
        return connection;
    }

    /**
     * get hiveDatabase
     * 
     * @return the hiveDatabase
     */
    public String getHiveDatabase() {
        return hiveDatabase;
    }

    /**
     * get producerContext
     * 
     * @return the producerContext
     */
    public Context getProducerContext() {
        return parentContext;
    }

    /**
     * get dispatchQueue
     * 
     * @return the dispatchQueue
     */
    public LinkedBlockingQueue<DispatchProfile> getDispatchQueue() {
        return dispatchQueue;
    }

    /**
     * getTopic
     * 
     * @param  uid
     * @return
     */
    public HdfsIdConfig getIdConfig(String uid) {
        return this.idConfigMap.get(uid);
    }

    /**
     * get hdfsPath
     * 
     * @return the hdfsPath
     */
    public String getHdfsPath() {
        return hdfsPath;
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
     * get maxFileOpenDelayMinute
     * 
     * @return the maxFileOpenDelayMinute
     */
    public long getMaxFileOpenDelayMinute() {
        return maxFileOpenDelayMinute;
    }

    /**
     * get fileArchiveDelayMinute
     * 
     * @return the fileArchiveDelayMinute
     */
    public long getFileArchiveDelayMinute() {
        return fileArchiveDelayMinute;
    }

    /**
     * get tokenOvertimeMinute
     * 
     * @return the tokenOvertimeMinute
     */
    public long getTokenOvertimeMinute() {
        return tokenOvertimeMinute;
    }

    /**
     * get maxOutputFileSizeGb
     * 
     * @return the maxOutputFileSizeGb
     */
    public long getMaxOutputFileSizeGb() {
        return maxOutputFileSizeGb;
    }

    /**
     * get idConfigMap
     * 
     * @return the idConfigMap
     */
    public Map<String, HdfsIdConfig> getIdConfigMap() {
        return idConfigMap;
    }

    /**
     * get outputPool
     * 
     * @return the outputPool
     */
    public ExecutorService getOutputPool() {
        return outputPool;
    }

    /**
     * get partitionCreatePool
     * 
     * @return the partitionCreatePool
     */
    public ExecutorService getPartitionCreatePool() {
        return partitionCreatePool;
    }

    /**
     * get eventFormatHandler
     * 
     * @return the eventFormatHandler
     */
    public IEventFormatHandler getEventFormatHandler() {
        return eventFormatHandler;
    }

    /**
     * get hiveJdbcUrl
     * 
     * @return the hiveJdbcUrl
     */
    public String getHiveJdbcUrl() {
        return hiveJdbcUrl;
    }

    /**
     * set hiveJdbcUrl
     * 
     * @param hiveJdbcUrl the hiveJdbcUrl to set
     */
    public void setHiveJdbcUrl(String hiveJdbcUrl) {
        this.hiveJdbcUrl = hiveJdbcUrl;
    }

    /**
     * get hiveUsername
     * 
     * @return the hiveUsername
     */
    public String getHiveUsername() {
        return hiveUsername;
    }

    /**
     * set hiveUsername
     * 
     * @param hiveUsername the hiveUsername to set
     */
    public void setHiveUsername(String hiveUsername) {
        this.hiveUsername = hiveUsername;
    }

    /**
     * get hivePassword
     * 
     * @return the hivePassword
     */
    public String getHivePassword() {
        return hivePassword;
    }

    /**
     * set hivePassword
     * 
     * @param hivePassword the hivePassword to set
     */
    public void setHivePassword(String hivePassword) {
        this.hivePassword = hivePassword;
    }

}
