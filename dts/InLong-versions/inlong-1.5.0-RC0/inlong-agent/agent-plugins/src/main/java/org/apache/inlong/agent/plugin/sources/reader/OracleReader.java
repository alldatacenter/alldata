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
import io.debezium.connector.oracle.OracleConnector;
import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.relational.history.FileDatabaseHistory;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.JobProfile;
import org.apache.inlong.agent.constant.AgentConstants;
import org.apache.inlong.agent.constant.OracleConstants;
import org.apache.inlong.agent.constant.SnapshotModeConstants;
import org.apache.inlong.agent.message.DefaultMessage;
import org.apache.inlong.agent.metrics.audit.AuditUtils;
import org.apache.inlong.agent.plugin.Message;
import org.apache.inlong.agent.plugin.sources.snapshot.OracleSnapshotBase;
import org.apache.inlong.agent.plugin.utils.InLongDatabaseHistory;
import org.apache.inlong.agent.plugin.utils.InLongFileOffsetBackingStore;
import org.apache.inlong.agent.pojo.DebeziumFormat;
import org.apache.inlong.agent.utils.AgentUtils;
import org.apache.inlong.agent.utils.GsonUtil;
import org.apache.kafka.connect.storage.FileOffsetBackingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import static org.apache.inlong.agent.constant.CommonConstants.DEFAULT_MAP_CAPACITY;
import static org.apache.inlong.agent.constant.CommonConstants.PROXY_KEY_DATA;

/**
 * Read data from Oracle database by Debezium
 */
public class OracleReader extends AbstractReader {

    public static final String ORACLE_READER_TAG_NAME = "AgentOracleMetric";
    public static final String JOB_DATABASE_USER = "job.oracleJob.user";
    public static final String JOB_DATABASE_PASSWORD = "job.oracleJob.password";
    public static final String JOB_DATABASE_HOSTNAME = "job.oracleJob.hostname";
    public static final String JOB_DATABASE_PORT = "job.oracleJob.port";
    public static final String JOB_DATABASE_SNAPSHOT_MODE = "job.oracleJob.snapshot.mode";
    public static final String JOB_DATABASE_SERVER_NAME = "job.oracleJob.serverName";
    public static final String JOB_DATABASE_QUEUE_SIZE = "job.oracleJob.queueSize";
    public static final String JOB_DATABASE_OFFSETS = "job.oracleJob.offsets";
    public static final String JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE = "job.oracleJob.offset.specificOffsetFile";
    public static final String JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS = "job.oracleJob.offset.specificOffsetPos";
    public static final String JOB_DATABASE_DBNAME = "job.oracleJob.dbname";
    public static final String JOB_DATABASE_STORE_OFFSET_INTERVAL_MS = "job.oracleJob.offset.intervalMs";
    public static final String JOB_DATABASE_STORE_HISTORY_FILENAME = "job.oracleJob.history.filename";

    private static final Gson GSON = new Gson();
    public static final String ORACLE = "oracle";

    private static final Logger LOGGER = LoggerFactory.getLogger(SqlReader.class);

    private final AgentConfiguration agentConf = AgentConfiguration.getAgentConf();

    private String databaseStoreHistoryName;
    private String instanceId;
    private String dbName;
    private String serverName;
    private String userName;
    private String password;
    private String hostName;
    private String port;
    private String offsetFlushIntervalMs;
    private String offsetStoreFileName;
    private String snapshotMode;
    private String offset;
    private String specificOffsetFile;
    private String specificOffsetPos;
    private OracleSnapshotBase oracleSnapshot;
    private boolean finished = false;
    private ExecutorService executor;

    /**
     * pair.left : table name
     * pair.right : actual data
     */
    private LinkedBlockingQueue<Pair<String, String>> oracleMessageQueue;
    private JobProfile jobProfile;
    private boolean destroyed = false;

    public OracleReader() {
    }

    @Override
    public Message read() {
        if (!oracleMessageQueue.isEmpty()) {
            return getOracleMessage();
        } else {
            return null;
        }
    }

    /**
     * poll message from buffer pool
     *
     * @return org.apache.inlong.agent.plugin.Message
     */
    private DefaultMessage getOracleMessage() {
        // Retrieves and removes the head of this queue,
        // or returns null if this queue is empty.
        Pair<String, String> message = oracleMessageQueue.poll();
        if (Objects.isNull(message)) {
            return null;
        }
        Map<String, String> header = new HashMap<>(DEFAULT_MAP_CAPACITY);
        header.put(PROXY_KEY_DATA, message.getKey());
        return new DefaultMessage(GsonUtil.toJson(message.getValue()).getBytes(StandardCharsets.UTF_8), header);
    }

    public boolean isDestroyed() {
        return destroyed;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public String getReadSource() {
        return instanceId;
    }

    @Override
    public void setReadTimeout(long mill) {

    }

    @Override
    public void setWaitMillisecond(long millis) {

    }

    @Override
    public String getSnapshot() {
        if (oracleSnapshot != null) {
            return oracleSnapshot.getSnapshot();
        } else {
            return StringUtils.EMPTY;
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

    private String tryToInitAndGetHistoryPath() {
        String historyPath = agentConf.get(
                AgentConstants.AGENT_HISTORY_PATH, AgentConstants.DEFAULT_AGENT_HISTORY_PATH);
        String parentPath = agentConf.get(
                AgentConstants.AGENT_HOME, AgentConstants.DEFAULT_AGENT_HOME);
        return AgentUtils.makeDirsIfNotExist(historyPath, parentPath).getAbsolutePath();
    }

    @Override
    public void init(JobProfile jobConf) {
        super.init(jobConf);
        jobProfile = jobConf;
        LOGGER.info("init oracle reader with jobConf {}", jobConf.toJsonStr());
        userName = jobConf.get(JOB_DATABASE_USER);
        password = jobConf.get(JOB_DATABASE_PASSWORD);
        hostName = jobConf.get(JOB_DATABASE_HOSTNAME);
        port = jobConf.get(JOB_DATABASE_PORT);
        dbName = jobConf.get(JOB_DATABASE_DBNAME);
        serverName = jobConf.get(JOB_DATABASE_SERVER_NAME);
        instanceId = jobConf.getInstanceId();
        offsetFlushIntervalMs = jobConf.get(JOB_DATABASE_STORE_OFFSET_INTERVAL_MS, "100000");
        offsetStoreFileName = jobConf.get(JOB_DATABASE_STORE_HISTORY_FILENAME,
                tryToInitAndGetHistoryPath()) + "/offset.dat" + instanceId;
        snapshotMode = jobConf.get(JOB_DATABASE_SNAPSHOT_MODE, OracleConstants.INITIAL);
        oracleMessageQueue = new LinkedBlockingQueue<>(jobConf.getInt(JOB_DATABASE_QUEUE_SIZE, 1000));
        finished = false;

        databaseStoreHistoryName = jobConf.get(JOB_DATABASE_STORE_HISTORY_FILENAME,
                tryToInitAndGetHistoryPath()) + "/history.dat" + jobConf.getInstanceId();
        offset = jobConf.get(JOB_DATABASE_OFFSETS, "");
        specificOffsetFile = jobConf.get(JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE, "");
        specificOffsetPos = jobConf.get(JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS, "-1");

        oracleSnapshot = new OracleSnapshotBase(offsetStoreFileName);
        oracleSnapshot.save(offset, oracleSnapshot.getFile());

        Properties props = getEngineProps();

        DebeziumEngine<ChangeEvent<String, String>> engine = DebeziumEngine.create(
                io.debezium.engine.format.Json.class)
                .using(props)
                .notifying((records, committer) -> {
                    try {
                        for (ChangeEvent<String, String> record : records) {
                            DebeziumFormat debeziumFormat = GSON
                                    .fromJson(record.value(), DebeziumFormat.class);
                            oracleMessageQueue.put(Pair.of(debeziumFormat.getSource().getTable(), record.value()));
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
                        LOGGER.error("oracle job with jobConf {} has error {}", instanceId, message, error);
                    }
                }).build();

        executor = Executors.newSingleThreadExecutor();
        executor.execute(engine);

        LOGGER.info("get initial snapshot of job {}, snapshot {}", instanceId, getSnapshot());
    }

    private Properties getEngineProps() {
        Properties props = new Properties();
        props.setProperty("name", "engine" + instanceId);
        props.setProperty("connector.class", OracleConnector.class.getCanonicalName());
        props.setProperty("database.hostname", hostName);
        props.setProperty("database.port", port);
        props.setProperty("database.user", userName);
        props.setProperty("database.password", password);
        props.setProperty("database.dbname", dbName);
        props.setProperty("database.server.name", serverName);
        props.setProperty("offset.flush.interval.ms", offsetFlushIntervalMs);
        props.setProperty("database.snapshot.mode", snapshotMode);
        props.setProperty("key.converter.schemas.enable", "false");
        props.setProperty("value.converter.schemas.enable", "false");
        props.setProperty("snapshot.mode", snapshotMode);
        props.setProperty("offset.storage.file.filename", offsetStoreFileName);
        props.setProperty("database.history.file.filename", databaseStoreHistoryName);
        if (SnapshotModeConstants.SPECIFIC_OFFSETS.equals(snapshotMode)) {
            Preconditions.checkNotNull(JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE,
                    JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_FILE + " cannot be null");
            Preconditions.checkNotNull(JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS,
                    JOB_DATABASE_OFFSET_SPECIFIC_OFFSET_POS + " cannot be null");
            props.setProperty("offset.storage", InLongFileOffsetBackingStore.class.getCanonicalName());
            props.setProperty(InLongFileOffsetBackingStore.OFFSET_STATE_VALUE,
                    serializeOffset(instanceId, specificOffsetFile, specificOffsetPos));
            props.setProperty("database.history", InLongDatabaseHistory.class.getCanonicalName());
        } else {
            props.setProperty("offset.storage", FileOffsetBackingStore.class.getCanonicalName());
            props.setProperty("database.history", FileDatabaseHistory.class.getCanonicalName());
        }
        props.setProperty("tombstones.on.delete", "false");
        props.setProperty("converters", "datetime");
        props.setProperty("datetime.type", "org.apache.inlong.agent.plugin.utils.BinlogTimeConverter");
        props.setProperty("datetime.format.date", "yyyy-MM-dd");
        props.setProperty("datetime.format.time", "HH:mm:ss");
        props.setProperty("datetime.format.datetime", "yyyy-MM-dd HH:mm:ss");
        props.setProperty("datetime.format.timestamp", "yyyy-MM-dd HH:mm:ss");

        LOGGER.info("oracle job {} start with props {}", jobProfile.getInstanceId(), props);
        return props;
    }

    @Override
    public void destroy() {
        synchronized (this) {
            if (!destroyed) {
                this.executor.shutdownNow();
                this.oracleSnapshot.close();
                this.destroyed = true;
            }
        }
    }
}
