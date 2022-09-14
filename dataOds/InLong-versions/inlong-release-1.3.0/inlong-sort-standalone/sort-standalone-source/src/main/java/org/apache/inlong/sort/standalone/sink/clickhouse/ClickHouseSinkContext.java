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

package org.apache.inlong.sort.standalone.sink.clickhouse;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang.ClassUtils;
import org.apache.commons.math3.util.Pair;
import org.apache.flume.Channel;
import org.apache.flume.Context;
import org.apache.inlong.common.pojo.sortstandalone.SortTaskConfig;
import org.apache.inlong.common.util.NetworkUtils;
import org.apache.inlong.sort.standalone.channel.ProfileEvent;
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
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 
 * ClickHouseSinkContext
 */
public class ClickHouseSinkContext extends SinkContext {

    public static final Logger LOG = InlongLoggerFactory.getLogger(ClickHouseSinkContext.class);
    public static final String KEY_NODE_ID = "nodeId";
    public static final String KEY_JDBC_DRIVER = "jdbcDriver";
    public static final String DEFAULT_JDBC_DRIVER = "ru.yandex.clickhouse.ClickHouseDriver";
    public static final String KEY_JDBC_URL = "jdbcUrl";
    public static final String KEY_JDBC_USERNAME = "jdbcUsername";
    public static final String KEY_JDBC_PASSWORD = "jdbcPassword";
    public static final String KEY_EVENT_HANDLER = "clickHouseEventHandler";

    private Context parentContext;
    private String nodeId;
    private Map<String, ClickHouseIdConfig> idConfigMap = new ConcurrentHashMap<>();
    private final LinkedBlockingQueue<DispatchProfile> dispatchQueue;
    // jdbc config
    private String jdbcDriver;
    private String jdbcUrl;
    private String jdbcUsername;
    private String jdbcPassword;

    /**
     * Constructor
     * 
     * @param sinkName
     * @param context
     * @param channel
     */
    public ClickHouseSinkContext(String sinkName, Context context, Channel channel,
            LinkedBlockingQueue<DispatchProfile> dispatchQueue) {
        super(sinkName, context, channel);
        this.parentContext = context;
        this.dispatchQueue = dispatchQueue;
        this.nodeId = CommonPropertiesHolder.getString(KEY_NODE_ID, NetworkUtils.getLocalIp());
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
            // parse the config of id and topic
            Map<String, ClickHouseIdConfig> newIdConfigMap = new ConcurrentHashMap<>();
            List<Map<String, String>> idList = newSortTaskConfig.getIdParams();
            ObjectMapper objectMapper = new ObjectMapper();
            for (Map<String, String> idParam : idList) {
                String inlongGroupId = idParam.get(Constants.INLONG_GROUP_ID);
                String inlongStreamId = idParam.get(Constants.INLONG_STREAM_ID);
                String uid = InlongId.generateUid(inlongGroupId, inlongStreamId);
                String jsonIdConfig = objectMapper.writeValueAsString(idParam);
                ClickHouseIdConfig idConfig = objectMapper.readValue(jsonIdConfig, ClickHouseIdConfig.class);
                newIdConfigMap.put(uid, idConfig);
            }
            // jdbc config
            Context currentContext = new Context(this.parentContext.getParameters());
            currentContext.putAll(newSortTaskConfig.getSinkParams());
            this.jdbcDriver = currentContext.getString(KEY_JDBC_DRIVER, DEFAULT_JDBC_DRIVER);
            this.jdbcUrl = currentContext.getString(KEY_JDBC_URL);
            this.jdbcUsername = currentContext.getString(KEY_JDBC_USERNAME);
            this.jdbcPassword = currentContext.getString(KEY_JDBC_PASSWORD);
            Class.forName(this.jdbcDriver);
            // load DB field
            this.initIdConfig(newIdConfigMap);
            // change current config
            this.sortTaskConfig = newSortTaskConfig;
            this.idConfigMap = newIdConfigMap;
            LOG.info("end to get SortTaskConfig,taskName:{},newIdConfigMap:{},currentContext:{}", taskName,
                    new ObjectMapper().writeValueAsString(newIdConfigMap), currentContext);
        } catch (Throwable e) {
            LOG.error(e.getMessage(), e);
        }
    }

    /**
     * initIdConfig
     * @param newIdConfigMap
     * @throws SQLException
     */
    private void initIdConfig(Map<String, ClickHouseIdConfig> newIdConfigMap) throws SQLException {
        try (Connection conn = DriverManager.getConnection(jdbcUrl, jdbcUsername, jdbcPassword);
                Statement stat = conn.createStatement();) {
            for (Entry<String, ClickHouseIdConfig> entry : newIdConfigMap.entrySet()) {
                // parse field list
                ClickHouseIdConfig idConfig = entry.getValue();
                idConfig.setContentFieldList(ClickHouseIdConfig.parseFieldNames(idConfig.getContentFieldNames()));
                // load db field type
                Map<String, Integer> fullTypeMap = new HashMap<>();
                try (ResultSet rs = stat.executeQuery("select * from " + idConfig.getTableName())) {
                    ResultSetMetaData meta = rs.getMetaData();
                    int columnCount = meta.getColumnCount();
                    for (int i = 1; i <= columnCount; i++) {
                        fullTypeMap.put(meta.getColumnName(i), meta.getColumnType(i));
                    }
                } catch (Exception e) {
                    LOG.error("Can not get metadata,group:{},stream:{},error:{}", idConfig.getInlongGroupId(),
                            idConfig.getInlongStreamId(), e.getMessage(), e);
                }
                // parse db field type
                List<String> dbFieldNameList = ClickHouseIdConfig.parseFieldNames(idConfig.getDbFieldNames());
                List<Pair<String, Integer>> dbFieldList = new ArrayList<>(dbFieldNameList.size());
                dbFieldNameList.forEach((fieldName) -> {
                    dbFieldList.add(new Pair<>(fieldName, fullTypeMap.getOrDefault(fieldName, Types.VARCHAR)));
                });
                idConfig.setDbFieldList(dbFieldList);
                // load db sql
                StringBuilder insertSql = new StringBuilder();
                insertSql.append("insert into ").append(idConfig.getTableName()).append(" (");
                idConfig.getDbFieldList().forEach((field) -> {
                    insertSql.append(field.getKey()).append(',');
                });
                insertSql.deleteCharAt(insertSql.length() - 1);
                insertSql.append(") values (");
                idConfig.getDbFieldList().forEach((field) -> {
                    insertSql.append("?,");
                });
                insertSql.deleteCharAt(insertSql.length() - 1);
                insertSql.append(")");
                idConfig.setInsertSql(insertSql.toString());
            }
        }
    }

    /**
     * addSendMetric
     * 
     * @param currentRecord
     */
    public void addSendMetric(DispatchProfile currentRecord) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        dimensions.put(SortMetricItem.KEY_SOURCE_ID, "-");
        dimensions.put(SortMetricItem.KEY_SOURCE_DATA_ID, "-");
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, "-");
        dimensions.put(SortMetricItem.KEY_INLONG_GROUP_ID, currentRecord.getInlongGroupId());
        dimensions.put(SortMetricItem.KEY_INLONG_STREAM_ID, currentRecord.getInlongStreamId());
        // msgTime
        long msgTime = currentRecord.getDispatchTime();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        // find metric
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        metricItem.sendCount.addAndGet(currentRecord.getCount());
        metricItem.sendSize.addAndGet(currentRecord.getSize());
    }

    /**
     * addReadFailMetric
     * @param errorMsg
     * @param currentRecord
     */
    public void addSendFailMetric(String errorMsg, DispatchProfile currentRecord) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        dimensions.put(SortMetricItem.KEY_SOURCE_ID, "-");
        dimensions.put(SortMetricItem.KEY_SOURCE_DATA_ID, "-");
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, errorMsg);
        dimensions.put(SortMetricItem.KEY_INLONG_GROUP_ID, currentRecord.getInlongGroupId());
        dimensions.put(SortMetricItem.KEY_INLONG_STREAM_ID, currentRecord.getInlongStreamId());
        // msgTime
        long msgTime = System.currentTimeMillis();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        // find metric
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        metricItem.readFailCount.addAndGet(currentRecord.getCount());
        metricItem.readFailSize.addAndGet(currentRecord.getSize());
    }

    /**
     * addReadFailMetric
     * @param errorMsg
     * @param event
     */
    public void addSendFailMetric(String errorMsg, ProfileEvent event) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        dimensions.put(SortMetricItem.KEY_SOURCE_ID, "-");
        dimensions.put(SortMetricItem.KEY_SOURCE_DATA_ID, "-");
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, errorMsg);
        dimensions.put(SortMetricItem.KEY_INLONG_GROUP_ID, event.getInlongGroupId());
        dimensions.put(SortMetricItem.KEY_INLONG_STREAM_ID, event.getInlongStreamId());
        // msgTime
        long msgTime = System.currentTimeMillis();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        // find metric
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        metricItem.readFailCount.incrementAndGet();
        metricItem.readFailSize.addAndGet(event.getBody().length);
    }

    /**
     * addReadFailMetric
     */
    public void addSendFailMetric(String errorMsg) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        dimensions.put(SortMetricItem.KEY_SOURCE_ID, "-");
        dimensions.put(SortMetricItem.KEY_SOURCE_DATA_ID, "-");
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, errorMsg);
        dimensions.put(SortMetricItem.KEY_INLONG_GROUP_ID, "-");
        dimensions.put(SortMetricItem.KEY_INLONG_STREAM_ID, "-");
        // msgTime
        long msgTime = System.currentTimeMillis();
        long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
        dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
        // find metric
        SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
        metricItem.readFailCount.incrementAndGet();
    }

    /**
     * addSendSuccessMetric
     * 
     * @param currentRecord
     * @param sendTime
     */
    public void addSendSuccessMetric(DispatchProfile currentRecord, long sendTime) {
        Map<String, String> dimensions = new HashMap<>();
        dimensions.put(SortMetricItem.KEY_CLUSTER_ID, this.getClusterId());
        dimensions.put(SortMetricItem.KEY_TASK_NAME, this.getTaskName());
        dimensions.put(SortMetricItem.KEY_SOURCE_ID, "-");
        dimensions.put(SortMetricItem.KEY_SOURCE_DATA_ID, "-");
        dimensions.put(SortMetricItem.KEY_SINK_ID, this.getSinkName());
        dimensions.put(SortMetricItem.KEY_SINK_DATA_ID, "-");
        dimensions.put(SortMetricItem.KEY_INLONG_GROUP_ID, currentRecord.getInlongGroupId());
        dimensions.put(SortMetricItem.KEY_INLONG_STREAM_ID, currentRecord.getInlongStreamId());
        long currentTime = System.currentTimeMillis();
        for (ProfileEvent event : currentRecord.getEvents()) {
            long msgTime = event.getRawLogTime();
            long auditFormatTime = msgTime - msgTime % CommonPropertiesHolder.getAuditFormatInterval();
            dimensions.put(SortMetricItem.KEY_MESSAGE_TIME, String.valueOf(auditFormatTime));
            SortMetricItem metricItem = this.getMetricItemSet().findMetricItem(dimensions);
            metricItem.sendSuccessCount.incrementAndGet();
            metricItem.sendSuccessSize.addAndGet(event.getBody().length);
            long sinkDuration = currentTime - sendTime;
            long nodeDuration = currentTime - event.getFetchTime();
            long wholeDuration = currentTime - msgTime;
            metricItem.sinkDuration.addAndGet(sinkDuration);
            metricItem.nodeDuration.addAndGet(nodeDuration);
            metricItem.wholeDuration.addAndGet(wholeDuration);
            AuditUtils.add(AuditUtils.AUDIT_ID_SEND_SUCCESS, event);
        }
    }

    /**
     * get nodeId
     * @return the nodeId
     */
    public String getNodeId() {
        return nodeId;
    }

    /**
     * set nodeId
     * @param nodeId the nodeId to set
     */
    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    /**
     * get jdbcDriver
     * @return the jdbcDriver
     */
    public String getJdbcDriver() {
        return jdbcDriver;
    }

    /**
     * set jdbcDriver
     * @param jdbcDriver the jdbcDriver to set
     */
    public void setJdbcDriver(String jdbcDriver) {
        this.jdbcDriver = jdbcDriver;
    }

    /**
     * get jdbcUrl
     * @return the jdbcUrl
     */
    public String getJdbcUrl() {
        return jdbcUrl;
    }

    /**
     * set jdbcUrl
     * @param jdbcUrl the jdbcUrl to set
     */
    public void setJdbcUrl(String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    /**
     * get jdbcUsername
     * @return the jdbcUsername
     */
    public String getJdbcUsername() {
        return jdbcUsername;
    }

    /**
     * set jdbcUsername
     * @param jdbcUsername the jdbcUsername to set
     */
    public void setJdbcUsername(String jdbcUsername) {
        this.jdbcUsername = jdbcUsername;
    }

    /**
     * get jdbcPassword
     * @return the jdbcPassword
     */
    public String getJdbcPassword() {
        return jdbcPassword;
    }

    /**
     * set jdbcPassword
     * @param jdbcPassword the jdbcPassword to set
     */
    public void setJdbcPassword(String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * get dispatchQueue
     * @return the dispatchQueue
     */
    public LinkedBlockingQueue<DispatchProfile> getDispatchQueue() {
        return dispatchQueue;
    }

    /**
     * getIdConfig
     * 
     * @param  uid
     * @return
     */
    public ClickHouseIdConfig getIdConfig(String uid) {
        return this.idConfigMap.get(uid);
    }

    /**
     * create createEventHandler
     * 
     * @return the IEventHandler
     */
    public IEventHandler createEventHandler() {
        // IEventHandler
        String eventHandlerClass = CommonPropertiesHolder.getString(KEY_EVENT_HANDLER,
                DefaultEventHandler.class.getName());
        try {
            Class<?> handlerClass = ClassUtils.getClass(eventHandlerClass);
            Object handlerObject = handlerClass.getDeclaredConstructor().newInstance();
            if (handlerObject instanceof IEventHandler) {
                IEventHandler handler = (IEventHandler) handlerObject;
                return handler;
            }
        } catch (Throwable t) {
            LOG.error("Fail to init IEventHandler,handlerClass:{},error:{}",
                    eventHandlerClass, t.getMessage(), t);
        }
        return null;
    }
}
