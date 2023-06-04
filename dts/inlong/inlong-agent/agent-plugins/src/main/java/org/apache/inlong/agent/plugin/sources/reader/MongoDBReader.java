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

package org.apache.inlong.agent.plugin.sources.reader;

import com.alibaba.fastjson.JSONPath;
import com.google.common.base.Preconditions;
import io.debezium.config.Configuration;
import io.debezium.config.Field;
import io.debezium.connector.mongodb.MongoDbConnector;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.SnapshotModeConstants;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.snapshot.MongoDBSnapshotBase;
import org.apache.inlong.agent.plugin.utils.InLongFileOffsetBackingStore;
import org.apache.inlong.agent.pojo.DebeziumFormat;
import org.apache.inlong.agent.utils.GsonUtil;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static io.debezium.connector.mongodb.MongoDbConnectorConfig.AUTO_DISCOVER_MEMBERS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.CAPTURE_MODE;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.COLLECTION_EXCLUDE_LIST;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.COLLECTION_INCLUDE_LIST;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.CONNECT_BACKOFF_INITIAL_DELAY_MS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.CONNECT_BACKOFF_MAX_DELAY_MS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.CONNECT_TIMEOUT_MS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.CURSOR_MAX_AWAIT_TIME_MS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.DATABASE_EXCLUDE_LIST;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.DATABASE_INCLUDE_LIST;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.FIELD_EXCLUDE_LIST;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.FIELD_RENAMES;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.HOSTS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.MAX_COPY_THREADS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.MAX_FAILED_CONNECTIONS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.MONGODB_POLL_INTERVAL_MS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.PASSWORD;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.SERVER_SELECTION_TIMEOUT_MS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.SNAPSHOT_MODE;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.SOCKET_TIMEOUT_MS;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.SSL_ALLOW_INVALID_HOSTNAMES;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.SSL_ENABLED;
import static io.debezium.connector.mongodb.MongoDbConnectorConfig.USER;
import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_MAP_CAPACITY;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_KEY_DATA;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_BACKOFF_INITIAL_DELAY;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_BACKOFF_MAX_DELAY;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_CAPTURE_MODE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_COLLECTION_EXCLUDE_LIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_COLLECTION_INCLUDE_LIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_CONNECT_MAX_ATTEMPTS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_CONNECT_TIMEOUT_MS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_CURSOR_MAX_AWAIT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_DATABASE_EXCLUDE_LIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_DATABASE_INCLUDE_LIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_FIELD_EXCLUDE_LIST;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_FIELD_RENAMES;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_HOSTS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_INITIAL_SYNC_MAX_THREADS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_MEMBERS_DISCOVER;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_OFFSETS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_OFFSET_SPECIFIC_OFFSET_FILE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_OFFSET_SPECIFIC_OFFSET_POS;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_PASSWORD;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_POLL_INTERVAL;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_QUEUE_SIZE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_SELECTION_TIMEOUT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_SNAPSHOT_MODE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_SOCKET_TIMEOUT;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_SSL_ENABLE;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_SSL_INVALID_HOSTNAME_ALLOWED;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_STORE_HISTORY_FILENAME;
import static org.apache.inlong.agent.constant.JobConstants.JOB_MONGO_USER;

/**
 * MongoDBReader : mongo source, split mongo source job into multi readers
 */
public class MongoDBReader extends AbstractReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDBReader.class);

    private String instanceId;
    private String offsetStoreFileName;
    private String specificOffsetFile;
    private String specificOffsetPos;
    private boolean finished = false;
    private boolean destroyed = false;

    private ExecutorService executor;
    /**
     * mongo snapshot info <br/>
     * Currently, there is no usage scenario
     */
    private MongoDBSnapshotBase snapshot;
    /**
     * message buffer queue
     */
    private LinkedBlockingQueue<Pair<String, DebeziumFormat>> bufferPool;

    @Override
    public Message read() {
        if (!bufferPool.isEmpty()) {
            return this.pollMessage();
        } else {
            return null;
        }
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getReadSource() {
        return instanceId;
    }

    public void setReadSource(String instanceId) {
        this.instanceId = instanceId;
    }

    @Override
    public void setReadTimeout(long mills) {
    }

    @Override
    public void setWaitMillisecond(long millis) {
    }

    @Override
    public String getSnapshot() {
        if (snapshot != null) {
            return snapshot.getSnapshot();
        } else {
            return "";
        }
    }

    @Override
    public void finishRead() {
        this.finished = true;
    }

    @Override
    public boolean isSourceExist() {
        return true;
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (!destroyed) {
                this.executor.shutdownNow();
                this.snapshot.close();
                this.destroyed = true;
            }
        }
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        this.setGlobalParamsValue(jobConf);
        this.startEmbeddedDebeziumEngine(jobConf);
    }

    /**
     * poll message from buffer pool
     *
     * @return org.apache.inlong.agent.plugin.Message
     */
    private Message pollMessage() {
        // Retrieves and removes the head of this queue,
        // or returns null if this queue is empty.
        Pair<String, DebeziumFormat> message = bufferPool.poll();
        if (message == null) {
            return null;
        }
        Map<String, String> header = new HashMap<>(DEFAULT_MAP_CAPACITY);
        header.put(PROXY_KEY_DATA, message.getKey());
        return new DefaultMessage(GsonUtil.toJson(message.getValue()).getBytes(StandardCharsets.UTF_8), header);
    }

    /**
     * set global parameters value
     *
     * @param jobConf job conf
     */
    private void setGlobalParamsValue(JobProfile jobConf) {
        bufferPool = new LinkedBlockingQueue<>(jobConf.getInt(JOB_MONGO_QUEUE_SIZE, 1000));
        instanceId = jobConf.getInstanceId();
        // offset file absolute path
        offsetStoreFileName = jobConf.get(JOB_MONGO_STORE_HISTORY_FILENAME,
                MongoDBSnapshotBase.getSnapshotFilePath()) + "/mongo-" + instanceId + "-offset.dat";
        // snapshot info
        snapshot = new MongoDBSnapshotBase(offsetStoreFileName);
        String offset = jobConf.get(JOB_MONGO_OFFSETS, "");
        snapshot.save(offset, new File(offsetStoreFileName));
        // offset info
        specificOffsetFile = jobConf.get(JOB_MONGO_OFFSET_SPECIFIC_OFFSET_FILE, "");
        specificOffsetPos = jobConf.get(JOB_MONGO_OFFSET_SPECIFIC_OFFSET_POS, "-1");
    }

    /**
     * start the embedded debezium engine
     *
     * @param jobConf job conf
     */
    private void startEmbeddedDebeziumEngine(JobProfile jobConf) {
        DebeziumEngine<ChangeEvent<String, String>> debeziumEngine = DebeziumEngine.create(Json.class)
                .using(this.buildMongoConnectorConfig(jobConf))
                .notifying(this::handleChangeEvent)
                .using(this::handle)
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.executor.execute(debeziumEngine);
    }

    /**
     * Handle the completion of the embedded connector engine.
     *
     * @param success {@code true} if the connector completed normally,
     *         or {@code false} if the connector produced an error
     *         that prevented startup or premature termination.
     * @param message the completion message; never null
     * @param error the error, or null if there was no exception
     */
    private void handle(boolean success, String message, Throwable error) {
        // jobConf.getInstanceId()
        if (!success) {
            LOGGER.error("MongoDB job with jobConf {} has error {}", message, error);
        }
    }

    /**
     * A Configuration object is basically a decorator around a {@link Properties} object.
     *
     * @return Configuration
     */
    private Properties buildMongoConnectorConfig(JobProfile jobConf) {
        Configuration.Builder builder = Configuration.create();
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_HOSTS, HOSTS);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_USER, USER);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_PASSWORD, PASSWORD);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_DATABASE_INCLUDE_LIST, DATABASE_INCLUDE_LIST);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_DATABASE_EXCLUDE_LIST, DATABASE_EXCLUDE_LIST);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_COLLECTION_INCLUDE_LIST, COLLECTION_INCLUDE_LIST);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_COLLECTION_EXCLUDE_LIST, COLLECTION_EXCLUDE_LIST);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_FIELD_EXCLUDE_LIST, FIELD_EXCLUDE_LIST);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_SNAPSHOT_MODE, SNAPSHOT_MODE);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_CAPTURE_MODE, CAPTURE_MODE);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_CONNECT_TIMEOUT_MS, CONNECT_TIMEOUT_MS);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_CURSOR_MAX_AWAIT, CURSOR_MAX_AWAIT_TIME_MS);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_SOCKET_TIMEOUT, SOCKET_TIMEOUT_MS);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_SELECTION_TIMEOUT, SERVER_SELECTION_TIMEOUT_MS);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_FIELD_RENAMES, FIELD_RENAMES);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_MEMBERS_DISCOVER, AUTO_DISCOVER_MEMBERS);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_CONNECT_MAX_ATTEMPTS, MAX_FAILED_CONNECTIONS);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_BACKOFF_MAX_DELAY, CONNECT_BACKOFF_MAX_DELAY_MS);
        setEngineConfigIfNecessary(jobConf, builder,
                JOB_MONGO_BACKOFF_INITIAL_DELAY, CONNECT_BACKOFF_INITIAL_DELAY_MS);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_INITIAL_SYNC_MAX_THREADS, MAX_COPY_THREADS);
        setEngineConfigIfNecessary(jobConf, builder,
                JOB_MONGO_SSL_INVALID_HOSTNAME_ALLOWED, SSL_ALLOW_INVALID_HOSTNAMES);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_SSL_ENABLE, SSL_ENABLED);
        setEngineConfigIfNecessary(jobConf, builder, JOB_MONGO_POLL_INTERVAL, MONGODB_POLL_INTERVAL_MS);

        Properties props = builder.build().asProperties();
        props.setProperty("offset.storage.file.filename", offsetStoreFileName);
        props.setProperty("connector.class", MongoDbConnector.class.getCanonicalName());
        props.setProperty("name", "engine-" + instanceId);
        props.setProperty("mongodb.name", "inlong-mongodb-" + instanceId);

        String snapshotMode = props.getOrDefault(JOB_MONGO_SNAPSHOT_MODE, "").toString();
        if (Objects.equals(SnapshotModeConstants.INITIAL, snapshotMode)) {
            Preconditions.checkNotNull(JOB_MONGO_OFFSET_SPECIFIC_OFFSET_FILE,
                    JOB_MONGO_OFFSET_SPECIFIC_OFFSET_FILE + " cannot be null");
            Preconditions.checkNotNull(JOB_MONGO_OFFSET_SPECIFIC_OFFSET_POS,
                    JOB_MONGO_OFFSET_SPECIFIC_OFFSET_POS + " cannot be null");
            props.setProperty("offset.storage", InLongFileOffsetBackingStore.class.getCanonicalName());
            props.setProperty(InLongFileOffsetBackingStore.OFFSET_STATE_VALUE,
                    serializeOffset(instanceId, specificOffsetFile, specificOffsetPos));
        } else {
            props.setProperty("offset.storage", FileOffsetBackingStore.class.getCanonicalName());
        }
        LOGGER.info("mongo job {} start with props {}",
                jobConf.getInstanceId(),
                GsonUtil.toJson(props));
        return props;
    }

    private void setEngineConfigIfNecessary(JobProfile jobConf,
            Configuration.Builder builder, String key, Field field) {
        String value = jobConf.get(key, field.defaultValueAsString());
        if (StringUtils.isBlank(value)) {
            return;
        }
        builder.with(field, value);
    }

    /**
     * Handles a batch of records, calling the {@link DebeziumEngine.RecordCommitter#markProcessed(Object)}
     * for each record and {@link DebeziumEngine.RecordCommitter#markBatchFinished()} when this batch is finished.
     *
     * @param records the records to be processed
     * @param committer the committer that indicates to the system that we are finished
     */
    private void handleChangeEvent(List<ChangeEvent<String, String>> records,
            DebeziumEngine.RecordCommitter<ChangeEvent<String, String>> committer) {
        try {
            for (ChangeEvent<String, String> record : records) {
                DebeziumFormat debeziumFormat = JSONPath.read(record.value(), "$.payload", DebeziumFormat.class);
                bufferPool.put(Pair.of(debeziumFormat.getSource().getCollection(), debeziumFormat));
                committer.markProcessed(record);
            }
            committer.markBatchFinished();
            long dataSize = records.stream().mapToLong(c -> c.value().length()).sum();
            AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS, super.inlongGroupId, super.inlongStreamId,
                    System.currentTimeMillis(), records.size(), dataSize);
            readerMetric.pluginReadSuccessCount.addAndGet(records.size());
            readerMetric.pluginReadCount.addAndGet(records.size());
        } catch (InterruptedException e) {
            e.printStackTrace();
            LOGGER.error("parse mongo message error", e);
            readerMetric.pluginReadFailCount.addAndGet(records.size());
            readerMetric.pluginReadCount.addAndGet(records.size());
        }
    }
}
