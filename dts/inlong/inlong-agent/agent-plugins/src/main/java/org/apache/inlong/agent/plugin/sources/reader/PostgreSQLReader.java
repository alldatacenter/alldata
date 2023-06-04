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

import com.google.common.base.Preconditions;
import com.google.gson.Gson;
import io.debezium.connector.postgresql.PostgresConnector;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.PostgreSQLConstants;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.snapshot.PostgreSQLSnapshotBase;
import org.apache.inlong.agent.plugin.utils.InLongFileOffsetBackingStore;
import org.apache.inlong.agent.pojo.DebeziumFormat;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_MAP_CAPACITY;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_KEY_DATA;

/**
 * Read postgreSQL data
 */
public class PostgreSQLReader extends AbstractReader {

    public static final String COMPONENT_NAME = "PostgreSQLReader";
    public static final String JOB_POSTGRESQL_USER = "job.postgreSQLJob.user";
    public static final String JOB_DATABASE_PASSWORD = "job.postgreSQLJob.password";
    public static final String JOB_DATABASE_HOSTNAME = "job.postgreSQLJob.hostname";
    public static final String JOB_DATABASE_PORT = "job.postgreSQLJob.port";
    public static final String JOB_DATABASE_STORE_OFFSET_INTERVAL_MS = "job.postgreSQLJob.offset.intervalMs";
    public static final String JOB_DATABASE_STORE_HISTORY_FILENAME = "job.postgreSQLJob.history.filename";
    public static final String JOB_DATABASE_SNAPSHOT_MODE = "job.postgreSQLJob.snapshot.mode";
    public static final String JOB_DATABASE_QUEUE_SIZE = "job.postgreSQLJob.queueSize";
    public static final String JOB_DATABASE_OFFSETS = "job.postgreSQLJob.offsets";
    public static final String JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE = "job.postgreSQLJob.offset.specificOffsetFile";
    public static final String JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS = "job.postgreSQLJob.offset.specificOffsetPos";
    public static final String JOB_DATABASE_DBNAME = "job.postgreSQLJob.dbname";
    public static final String JOB_DATABASE_SERVER_NAME = "job.postgreSQLJob.servername";
    public static final String JOB_DATABASE_PLUGIN_NAME = "job.postgreSQLJob.pluginname";
    private static final Gson GSON = new Gson();
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgreSQLReader.class);
    private final AgentConfiguration agentConf = AgentConfiguration.getAgentConf();
    private String userName;
    private String password;
    private String hostName;
    private String port;
    private String offsetFlushIntervalMs;
    private String offsetStoreFileName;
    private String snapshotMode;
    private String instanceId;
    private String offset;
    private String specificOffsetFile;
    private String specificOffsetPos;
    private String dbName;
    private String pluginName;
    private String serverName;
    private PostgreSQLSnapshotBase postgreSQLSnapshot;
    private boolean finished = false;
    private ExecutorService executor;
    /**
     * pair.left : table name
     * pair.right : actual data
     */
    private LinkedBlockingQueue<Pair<String, String>> postgreSQLMessageQueue;
    private JobProfile jobProfile;
    private boolean destroyed = false;

    public PostgreSQLReader() {
    }

    @Override
    public Message read() {
        if (!postgreSQLMessageQueue.isEmpty()) {
            return getPostgreSQLMessage();
        } else {
            return null;
        }
    }

    private DefaultMessage getPostgreSQLMessage() {
        Pair<String, String> message = postgreSQLMessageQueue.poll();
        Map<String, String> header = new HashMap<>(DEFAULT_MAP_CAPACITY);
        header.put(PROXY_KEY_DATA, message.getKey());
        return new DefaultMessage(message.getValue().getBytes(StandardCharsets.UTF_8), header);
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        jobProfile = jobConf;
        LOGGER.info("init PostgreSQL reader with jobConf {}", jobConf.toJsonStr());
        userName = jobConf.get(JOB_POSTGRESQL_USER);
        password = jobConf.get(JOB_DATABASE_PASSWORD);
        hostName = jobConf.get(JOB_DATABASE_HOSTNAME);
        port = jobConf.get(JOB_DATABASE_PORT);
        dbName = jobConf.get(JOB_DATABASE_DBNAME);
        serverName = jobConf.get(JOB_DATABASE_SERVER_NAME);
        pluginName = jobConf.get(JOB_DATABASE_PLUGIN_NAME, "pgoutput");
        instanceId = jobConf.getInstanceId();
        offsetFlushIntervalMs = jobConf.get(JOB_DATABASE_STORE_OFFSET_INTERVAL_MS, "100000");
        offsetStoreFileName = jobConf.get(JOB_DATABASE_STORE_HISTORY_FILENAME,
                tryToInitAndGetHistoryPath()) + "/offset.dat" + instanceId;
        snapshotMode = jobConf.get(JOB_DATABASE_SNAPSHOT_MODE, PostgreSQLConstants.INITIAL);
        postgreSQLMessageQueue = new LinkedBlockingQueue<>(jobConf.getInt(JOB_DATABASE_QUEUE_SIZE, 1000));
        finished = false;

        offset = jobConf.get(JOB_DATABASE_OFFSETS, "");
        specificOffsetFile = jobConf.get(JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE, "");
        specificOffsetPos = jobConf.get(JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS, "-1");
        postgreSQLSnapshot = new PostgreSQLSnapshotBase(offsetStoreFileName);
        postgreSQLSnapshot.save(offset, postgreSQLSnapshot.getFile());

        Properties props = getEngineProps();

        DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(
                io.debezium.engine.format.Json.class)
                .using(props)
                .notifying((records, committer) -> {
                    try {
                        for (ChangeEvent<String, String> record : records) {
                            DebeziumFormat debeziumFormat = GSON
                                    .fromJson(record.value(), DebeziumFormat.class);
                            postgreSQLMessageQueue.put(Pair.of(debeziumFormat.getSource().getTable(), record.value()));
                            committer.markProcessed(record);
                        }
                        committer.markBatchFinished();
                        long dataSize = records.stream().mapToLong(c -> c.value().length()).sum();
                        AuditUtils.add(AuditUtils.AUDIT_ID_AGENT_READ_SUCCESS, inlongGroupId, inlongStreamId,
                                System.currentTimeMillis(), records.size(), dataSize);
                        readerMetric.pluginReadSuccessCount.addAndGet(records.size());
                        readerMetric.pluginReadCount.addAndGet(records.size());
                    } catch (Exception e) {
                        readerMetric.pluginReadFailCount.addAndGet(records.size());
                        readerMetric.pluginReadCount.addAndGet(records.size());
                        LOGGER.error("parse binlog message error", e);
                    }
                })
                .using((success, message, error) -> {
                    if (!success) {
                        LOGGER.error("PostgreSQL job with jobConf {} has error {}", instanceId, message, error);
                    }
                }).build();

        executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);

        LOGGER.info("get initial snapshot of job {}, snapshot {}", instanceId, getSnapshot());
    }

    private String tryToInitAndGetHistoryPath() {
        String historyPath = agentConf.get(
                AgentConstants.AGENT_HISTORY_PATH, AgentConstants.DEFAULT_AGENT_HISTORY_PATH);
        String parentPath = agentConf.get(
                AgentConstants.AGENT_HOME, AgentConstants.DEFAULT_AGENT_HOME);
        return AgentUtils.makeDirsIfNotExist(historyPath, parentPath).getAbsolutePath();
    }

    private Properties getEngineProps() {
        Properties props = new Properties();

        props.setProperty("name", "engine" + instanceId);
        props.setProperty("connector.class", PostgresConnector.class.getCanonicalName());
        props.setProperty("database.server.name", serverName);
        props.setProperty("plugin.name", pluginName);
        props.setProperty("slot.name", "slot" + instanceId);
        props.setProperty("database.hostname", hostName);
        props.setProperty("database.port", port);
        props.setProperty("database.user", userName);
        props.setProperty("database.dbname", dbName);
        props.setProperty("database.password", password);

        props.setProperty("offset.flush.interval.ms", offsetFlushIntervalMs);
        props.setProperty("database.snapshot.mode", snapshotMode);
        props.setProperty("key.converter.schemas.enable", "false");
        props.setProperty("value.converter.schemas.enable", "false");
        props.setProperty("snapshot.mode", snapshotMode);
        props.setProperty("offset.storage.file.filename", offsetStoreFileName);
        if (PostgreSQLConstants.CUSTOM.equals(snapshotMode)) {
            Preconditions.checkNotNull(JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE,
                    JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE + " cannot be null");
            Preconditions.checkNotNull(JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS,
                    JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS + " cannot be null");
            props.setProperty("offset.storage", InLongFileOffsetBackingStore.class.getCanonicalName());
            props.setProperty(InLongFileOffsetBackingStore.OFFSET_STATE_VALUE,
                    serializeOffset(instanceId, specificOffsetFile, specificOffsetPos));
        } else {
            props.setProperty("offset.storage", FileOffsetBackingStore.class.getCanonicalName());
        }
        props.setProperty("tombstones.on.delete", "false");
        props.setProperty("converters", "datetime");
        props.setProperty("datetime.type", "org.apache.inlong.agent.plugin.utils.BinlogTimeConverter");
        props.setProperty("datetime.format.date", "yyyy-MM-dd");
        props.setProperty("datetime.format.time", "HH:mm:ss");
        props.setProperty("datetime.format.datetime", "yyyy-MM-dd HH:mm:ss");
        props.setProperty("datetime.format.timestamp", "yyyy-MM-dd HH:mm:ss");

        LOGGER.info("PostgreSQL job {} start with props {}", jobProfile.getInstanceId(), props);
        return props;
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (!destroyed) {
                executor.shutdownNow();
                postgreSQLSnapshot.close();
                destroyed = true;
            }
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
    public void setReadTimeout(long mill) {
        return;
    }

    @Override
    public void setWaitMillisecond(long millis) {
        return;
    }

    @Override
    public String getSnapshot() {
        if (postgreSQLSnapshot != null) {
            return postgreSQLSnapshot.getSnapshot();
        }
        return "";
    }

    @Override
    public void finishRead() {
        finished = true;
    }

    @Override
    public boolean isSourceExist() {
        return true;
    }
}
