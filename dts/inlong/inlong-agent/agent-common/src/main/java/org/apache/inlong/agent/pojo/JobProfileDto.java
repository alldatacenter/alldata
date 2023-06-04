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

package org.apache.inlong.agent.pojo;

import com.google.gson.Gson;
import lombok.Data;
import org.apache.inlong.agent.conf.AgentConfiguration;
import org.apache.inlong.agent.conf.TriggerProfile;
import org.apache.inlong.agent.pojo.FileJob.Line;
import org.apache.inlong.common.constant.MQType;
import org.apache.inlong.common.enums.TaskTypeEnum;
import org.apache.inlong.common.pojo.agent.DataConfig;

import static java.util.Objects.requireNonNull;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_HOST;
import static org.apache.inlong.agent.constant.FetcherConstants.AGENT_MANAGER_VIP_HTTP_PORT;
import static org.apache.inlong.agent.constant.JobConstants.SYNC_SEND_OPEN;

@Data
public class JobProfileDto {

    public static final String DEFAULT_TRIGGER = "org.apache.inlong.agent.plugin.trigger.DirectoryTrigger";
    public static final String DEFAULT_CHANNEL = "org.apache.inlong.agent.plugin.channel.MemoryChannel";
    public static final String MANAGER_JOB = "MANAGER_JOB";
    public static final String DEFAULT_DATAPROXY_SINK = "org.apache.inlong.agent.plugin.sinks.ProxySink";
    public static final String PULSAR_SINK = "org.apache.inlong.agent.plugin.sinks.PulsarSink";
    public static final String KAFKA_SINK = "org.apache.inlong.agent.plugin.sinks.KafkaSink";

    /**
     * file source
     */
    public static final String DEFAULT_SOURCE = "org.apache.inlong.agent.plugin.sources.TextFileSource";
    /**
     * binlog source
     */
    public static final String BINLOG_SOURCE = "org.apache.inlong.agent.plugin.sources.BinlogSource";
    /**
     * kafka source
     */
    public static final String KAFKA_SOURCE = "org.apache.inlong.agent.plugin.sources.KafkaSource";
    /**
     * PostgreSQL source
     */
    public static final String POSTGRESQL_SOURCE = "org.apache.inlong.agent.plugin.sources.PostgreSQLSource";
    /**
     * mongo source
     */
    public static final String MONGO_SOURCE = "org.apache.inlong.agent.plugin.sources.MongoDBSource";
    /**
     * oracle source
     */
    public static final String ORACLE_SOURCE = "org.apache.inlong.agent.plugin.sources.OracleSource";
    /**
     * redis source
     */
    public static final String REDIS_SOURCE = "org.apache.inlong.agent.plugin.sources.RedisSource";
    /**
     * mqtt source
     */
    public static final String MQTT_SOURCE = "org.apache.inlong.agent.plugin.sources.MqttSource";
    /**
     * sqlserver source
     */
    public static final String SQLSERVER_SOURCE = "org.apache.inlong.agent.plugin.sources.SQLServerSource";

    private static final Gson GSON = new Gson();

    private Job job;
    private Proxy proxy;

    private static BinlogJob getBinlogJob(DataConfig dataConfigs) {
        BinlogJob.BinlogJobTaskConfig binlogJobTaskConfig = GSON.fromJson(dataConfigs.getExtParams(),
                BinlogJob.BinlogJobTaskConfig.class);

        BinlogJob binlogJob = new BinlogJob();
        binlogJob.setHostname(binlogJobTaskConfig.getHostname());
        binlogJob.setPassword(binlogJobTaskConfig.getPassword());
        binlogJob.setUser(binlogJobTaskConfig.getUser());
        binlogJob.setTableWhiteList(binlogJobTaskConfig.getTableWhiteList());
        binlogJob.setDatabaseWhiteList(binlogJobTaskConfig.getDatabaseWhiteList());
        binlogJob.setSchema(binlogJobTaskConfig.getIncludeSchema());
        binlogJob.setPort(binlogJobTaskConfig.getPort());
        binlogJob.setOffsets(dataConfigs.getSnapshot());
        binlogJob.setDdl(binlogJobTaskConfig.getMonitoredDdl());
        binlogJob.setServerTimezone(binlogJobTaskConfig.getServerTimezone());

        BinlogJob.Offset offset = new BinlogJob.Offset();
        offset.setIntervalMs(binlogJobTaskConfig.getIntervalMs());
        offset.setFilename(binlogJobTaskConfig.getOffsetFilename());
        offset.setSpecificOffsetFile(binlogJobTaskConfig.getSpecificOffsetFile());
        offset.setSpecificOffsetPos(binlogJobTaskConfig.getSpecificOffsetPos());

        binlogJob.setOffset(offset);

        BinlogJob.Snapshot snapshot = new BinlogJob.Snapshot();
        snapshot.setMode(binlogJobTaskConfig.getSnapshotMode());

        binlogJob.setSnapshot(snapshot);

        BinlogJob.History history = new BinlogJob.History();
        history.setFilename(binlogJobTaskConfig.getHistoryFilename());

        binlogJob.setHistory(history);

        return binlogJob;
    }

    private static FileJob getFileJob(DataConfig dataConfigs) {
        FileJob fileJob = new FileJob();
        fileJob.setId(dataConfigs.getTaskId());
        fileJob.setTrigger(DEFAULT_TRIGGER);

        FileJob.FileJobTaskConfig fileJobTaskConfig = GSON.fromJson(dataConfigs.getExtParams(),
                FileJob.FileJobTaskConfig.class);

        FileJob.Dir dir = new FileJob.Dir();
        dir.setPatterns(fileJobTaskConfig.getPattern());
        dir.setBlackList(fileJobTaskConfig.getBlackList());
        fileJob.setDir(dir);
        fileJob.setCollectType(fileJobTaskConfig.getCollectType());
        fileJob.setContentCollectType(fileJobTaskConfig.getContentCollectType());
        fileJob.setDataSeparator(fileJobTaskConfig.getDataSeparator());
        fileJob.setProperties(GSON.toJson(fileJobTaskConfig.getProperties()));
        if (fileJobTaskConfig.getTimeOffset() != null) {
            fileJob.setTimeOffset(fileJobTaskConfig.getTimeOffset());
        }

        if (fileJobTaskConfig.getAdditionalAttr() != null) {
            fileJob.setAddictiveString(fileJobTaskConfig.getAdditionalAttr());
        }

        if (null != fileJobTaskConfig.getLineEndPattern()) {
            FileJob.Line line = new Line();
            line.setEndPattern(fileJobTaskConfig.getLineEndPattern());
            fileJob.setLine(line);
        }

        if (null != fileJobTaskConfig.getEnvList()) {
            fileJob.setEnvList(fileJobTaskConfig.getEnvList());
        }

        if (null != fileJobTaskConfig.getMetaFields()) {
            fileJob.setMetaFields(GSON.toJson(fileJobTaskConfig.getMetaFields()));
        }

        if (null != fileJobTaskConfig.getFilterMetaByLabels()) {
            fileJob.setFilterMetaByLabels(GSON.toJson(fileJobTaskConfig.getFilterMetaByLabels()));
        }

        if (null != fileJobTaskConfig.getMonitorInterval()) {
            fileJob.setMonitorInterval(fileJobTaskConfig.getMonitorInterval());
        }

        if (null != fileJobTaskConfig.getMonitorStatus()) {
            fileJob.setMonitorStatus(fileJobTaskConfig.getMonitorStatus());
        }
        return fileJob;
    }

    private static KafkaJob getKafkaJob(DataConfig dataConfigs) {

        KafkaJob.KafkaJobTaskConfig kafkaJobTaskConfig = GSON.fromJson(dataConfigs.getExtParams(),
                KafkaJob.KafkaJobTaskConfig.class);
        KafkaJob kafkaJob = new KafkaJob();

        KafkaJob.Bootstrap bootstrap = new KafkaJob.Bootstrap();
        bootstrap.setServers(kafkaJobTaskConfig.getBootstrapServers());
        kafkaJob.setBootstrap(bootstrap);
        KafkaJob.Partition partition = new KafkaJob.Partition();
        partition.setOffset(dataConfigs.getSnapshot());
        kafkaJob.setPartition(partition);
        KafkaJob.Group group = new KafkaJob.Group();
        group.setId(kafkaJobTaskConfig.getGroupId());
        kafkaJob.setGroup(group);
        KafkaJob.RecordSpeed recordSpeed = new KafkaJob.RecordSpeed();
        recordSpeed.setLimit(kafkaJobTaskConfig.getRecordSpeedLimit());
        kafkaJob.setRecordSpeed(recordSpeed);
        KafkaJob.ByteSpeed byteSpeed = new KafkaJob.ByteSpeed();
        byteSpeed.setLimit(kafkaJobTaskConfig.getByteSpeedLimit());
        kafkaJob.setByteSpeed(byteSpeed);
        kafkaJob.setAutoOffsetReset(kafkaJobTaskConfig.getAutoOffsetReset());

        kafkaJob.setTopic(kafkaJobTaskConfig.getTopic());

        return kafkaJob;
    }

    private static PostgreSQLJob getPostgresJob(DataConfig dataConfigs) {
        PostgreSQLJob.PostgreSQLJobConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                PostgreSQLJob.PostgreSQLJobConfig.class);
        PostgreSQLJob postgreSQLJob = new PostgreSQLJob();

        postgreSQLJob.setUser(config.getUsername());
        postgreSQLJob.setPassword(config.getPassword());
        postgreSQLJob.setHostname(config.getHostname());
        postgreSQLJob.setPort(config.getPort());
        postgreSQLJob.setDbname(config.getDatabase());
        postgreSQLJob.setServername(config.getSchema());
        postgreSQLJob.setPluginname(config.getDecodingPluginName());
        postgreSQLJob.setTableNameList(config.getTableNameList());
        postgreSQLJob.setServerTimeZone(config.getServerTimeZone());
        postgreSQLJob.setScanStartupMode(config.getScanStartupMode());
        postgreSQLJob.setPrimaryKey(config.getPrimaryKey());

        return postgreSQLJob;
    }

    private static RedisJob getRedisJob(DataConfig dataConfig) {
        RedisJob.RedisJobConfig config = GSON.fromJson(dataConfig.getExtParams(), RedisJob.RedisJobConfig.class);
        RedisJob redisJob = new RedisJob();

        redisJob.setAuthUser(config.getUsername());
        redisJob.setAuthPassword(config.getPassword());
        redisJob.setHostname(config.getHostname());
        redisJob.setPort(config.getPort());
        redisJob.setSsl(config.getSsl());
        redisJob.setReadTimeout(config.getTimeout());
        redisJob.setQueueSize(config.getQueueSize());
        redisJob.setReplId(config.getReplId());

        return redisJob;
    }

    private static MongoJob getMongoJob(DataConfig dataConfigs) {

        MongoJob.MongoJobTaskConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                MongoJob.MongoJobTaskConfig.class);
        MongoJob mongoJob = new MongoJob();

        mongoJob.setHosts(config.getHosts());
        mongoJob.setUser(config.getUsername());
        mongoJob.setPassword(config.getPassword());
        mongoJob.setDatabaseIncludeList(config.getDatabaseIncludeList());
        mongoJob.setDatabaseExcludeList(config.getDatabaseExcludeList());
        mongoJob.setCollectionIncludeList(config.getCollectionIncludeList());
        mongoJob.setCollectionExcludeList(config.getCollectionExcludeList());
        mongoJob.setFieldExcludeList(config.getFieldExcludeList());
        mongoJob.setConnectTimeoutInMs(config.getConnectTimeoutInMs());
        mongoJob.setQueueSize(config.getQueueSize());
        mongoJob.setCursorMaxAwaitTimeInMs(config.getCursorMaxAwaitTimeInMs());
        mongoJob.setSocketTimeoutInMs(config.getSocketTimeoutInMs());
        mongoJob.setSelectionTimeoutInMs(config.getSelectionTimeoutInMs());
        mongoJob.setFieldRenames(config.getFieldRenames());
        mongoJob.setMembersAutoDiscover(config.getMembersAutoDiscover());
        mongoJob.setConnectMaxAttempts(config.getConnectMaxAttempts());
        mongoJob.setConnectBackoffMaxDelayInMs(config.getConnectBackoffMaxDelayInMs());
        mongoJob.setConnectBackoffInitialDelayInMs(config.getConnectBackoffInitialDelayInMs());
        mongoJob.setInitialSyncMaxThreads(config.getInitialSyncMaxThreads());
        mongoJob.setSslInvalidHostnameAllowed(config.getSslInvalidHostnameAllowed());
        mongoJob.setSslEnabled(config.getSslEnabled());
        mongoJob.setPollIntervalInMs(config.getPollIntervalInMs());

        MongoJob.Offset offset = new MongoJob.Offset();
        offset.setFilename(config.getOffsetFilename());
        offset.setSpecificOffsetFile(config.getSpecificOffsetFile());
        offset.setSpecificOffsetPos(config.getSpecificOffsetPos());
        mongoJob.setOffset(offset);

        MongoJob.Snapshot snapshot = new MongoJob.Snapshot();
        snapshot.setMode(config.getSnapshotMode());
        mongoJob.setSnapshot(snapshot);

        MongoJob.History history = new MongoJob.History();
        history.setFilename(config.getHistoryFilename());
        mongoJob.setHistory(history);

        return mongoJob;
    }

    private static OracleJob getOracleJob(DataConfig dataConfigs) {
        OracleJob.OracleJobConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                OracleJob.OracleJobConfig.class);
        OracleJob oracleJob = new OracleJob();
        oracleJob.setUser(config.getUser());
        oracleJob.setHostname(config.getHostname());
        oracleJob.setPassword(config.getPassword());
        oracleJob.setPort(config.getPort());
        oracleJob.setServerName(config.getServerName());
        oracleJob.setDbname(config.getDbname());

        OracleJob.Offset offset = new OracleJob.Offset();
        offset.setFilename(config.getOffsetFilename());
        offset.setSpecificOffsetFile(config.getSpecificOffsetFile());
        offset.setSpecificOffsetPos(config.getSpecificOffsetPos());
        oracleJob.setOffset(offset);

        OracleJob.Snapshot snapshot = new OracleJob.Snapshot();
        snapshot.setMode(config.getSnapshotMode());
        oracleJob.setSnapshot(snapshot);

        OracleJob.History history = new OracleJob.History();
        history.setFilename(config.getHistoryFilename());
        oracleJob.setHistory(history);

        return oracleJob;
    }

    private static SqlServerJob getSqlServerJob(DataConfig dataConfigs) {
        SqlServerJob.SqlserverJobConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                SqlServerJob.SqlserverJobConfig.class);
        SqlServerJob sqlServerJob = new SqlServerJob();
        sqlServerJob.setUser(config.getUsername());
        sqlServerJob.setHostname(config.getHostname());
        sqlServerJob.setPassword(config.getPassword());
        sqlServerJob.setPort(config.getPort());
        sqlServerJob.setServerName(config.getSchemaName());
        sqlServerJob.setDbname(config.getDatabase());

        SqlServerJob.Offset offset = new SqlServerJob.Offset();
        offset.setFilename(config.getOffsetFilename());
        offset.setSpecificOffsetFile(config.getSpecificOffsetFile());
        offset.setSpecificOffsetPos(config.getSpecificOffsetPos());
        sqlServerJob.setOffset(offset);

        SqlServerJob.Snapshot snapshot = new SqlServerJob.Snapshot();
        snapshot.setMode(config.getSnapshotMode());
        sqlServerJob.setSnapshot(snapshot);

        SqlServerJob.History history = new SqlServerJob.History();
        history.setFilename(config.getHistoryFilename());
        sqlServerJob.setHistory(history);

        return sqlServerJob;
    }

    public static MqttJob getMqttJob(DataConfig dataConfigs) {
        MqttJob.MqttJobConfig config = GSON.fromJson(dataConfigs.getExtParams(),
                MqttJob.MqttJobConfig.class);
        MqttJob mqttJob = new MqttJob();

        mqttJob.setServerURI(config.getServerURI());
        mqttJob.setUserName(config.getUsername());
        mqttJob.setPassword(config.getPassword());
        mqttJob.setTopic(config.getTopic());
        mqttJob.setConnectionTimeOut(config.getConnectionTimeOut());
        mqttJob.setKeepAliveInterval(config.getKeepAliveInterval());
        mqttJob.setQos(config.getQos());
        mqttJob.setCleanSession(config.getCleanSession());
        mqttJob.setClientIdPrefix(config.getClientId());
        mqttJob.setQueueSize(config.getQueueSize());
        mqttJob.setAutomaticReconnect(config.getAutomaticReconnect());
        mqttJob.setMqttVersion(config.getMqttVersion());

        return mqttJob;
    }

    private static Proxy getProxy(DataConfig dataConfigs) {
        Proxy proxy = new Proxy();
        Manager manager = new Manager();
        AgentConfiguration agentConf = AgentConfiguration.getAgentConf();
        manager.setHost(agentConf.get(AGENT_MANAGER_VIP_HTTP_HOST));
        manager.setPort(agentConf.get(AGENT_MANAGER_VIP_HTTP_PORT));
        proxy.setInlongGroupId(dataConfigs.getInlongGroupId());
        proxy.setInlongStreamId(dataConfigs.getInlongStreamId());
        proxy.setManager(manager);
        if (null != dataConfigs.getSyncSend()) {
            proxy.setSync(dataConfigs.getSyncSend() == SYNC_SEND_OPEN);
        }
        if (null != dataConfigs.getSyncPartitionKey()) {
            proxy.setPartitionKey(dataConfigs.getSyncPartitionKey());
        }
        return proxy;
    }

    /**
     * convert DataConfig to TriggerProfile
     */
    public static TriggerProfile convertToTriggerProfile(DataConfig dataConfig) {
        if (!dataConfig.isValid()) {
            throw new IllegalArgumentException("input dataConfig" + dataConfig + "is invalid please check");
        }

        JobProfileDto profileDto = new JobProfileDto();
        Proxy proxy = getProxy(dataConfig);
        profileDto.setProxy(proxy);
        Job job = new Job();

        // common attribute
        job.setId(String.valueOf(dataConfig.getTaskId()));
        job.setGroupId(dataConfig.getInlongGroupId());
        job.setStreamId(dataConfig.getInlongStreamId());
        job.setChannel(DEFAULT_CHANNEL);
        job.setIp(dataConfig.getIp());
        job.setOp(dataConfig.getOp());
        job.setDeliveryTime(dataConfig.getDeliveryTime());
        job.setUuid(dataConfig.getUuid());
        job.setVersion(dataConfig.getVersion());
        // set sink type
        if (dataConfig.getDataReportType() == 0) {
            job.setSink(DEFAULT_DATAPROXY_SINK);
            job.setProxySend(false);
        } else if (dataConfig.getDataReportType() == 1) {
            job.setSink(DEFAULT_DATAPROXY_SINK);
            job.setProxySend(true);
        } else {
            String mqType = dataConfig.getMqClusters().get(0).getMqType();
            job.setMqClusters(GSON.toJson(dataConfig.getMqClusters()));
            job.setTopicInfo(GSON.toJson(dataConfig.getTopicInfo()));
            if (mqType.equals(MQType.PULSAR)) {
                job.setSink(PULSAR_SINK);
            } else if (mqType.equals(MQType.KAFKA)) {
                job.setSink(KAFKA_SINK);
            } else {
                throw new IllegalArgumentException("input dataConfig" + dataConfig + "is invalid please check");
            }
        }
        TaskTypeEnum taskType = TaskTypeEnum.getTaskType(dataConfig.getTaskType());
        switch (requireNonNull(taskType)) {
            case SQL:
            case BINLOG:
                BinlogJob binlogJob = getBinlogJob(dataConfig);
                job.setBinlogJob(binlogJob);
                job.setSource(BINLOG_SOURCE);
                profileDto.setJob(job);
                break;
            case FILE:
                FileJob fileJob = getFileJob(dataConfig);
                job.setFileJob(fileJob);
                job.setSource(DEFAULT_SOURCE);
                profileDto.setJob(job);
                break;
            case KAFKA:
                KafkaJob kafkaJob = getKafkaJob(dataConfig);
                job.setKafkaJob(kafkaJob);
                job.setSource(KAFKA_SOURCE);
                profileDto.setJob(job);
                break;
            case POSTGRES:
                PostgreSQLJob postgreSQLJob = getPostgresJob(dataConfig);
                job.setPostgreSQLJob(postgreSQLJob);
                job.setSource(POSTGRESQL_SOURCE);
                profileDto.setJob(job);
                break;
            case ORACLE:
                OracleJob oracleJob = getOracleJob(dataConfig);
                job.setOracleJob(oracleJob);
                job.setSource(ORACLE_SOURCE);
                profileDto.setJob(job);
                break;
            case SQLSERVER:
                SqlServerJob sqlserverJob = getSqlServerJob(dataConfig);
                job.setSqlserverJob(sqlserverJob);
                job.setSource(SQLSERVER_SOURCE);
                profileDto.setJob(job);
                break;
            case MONGODB:
                MongoJob mongoJob = getMongoJob(dataConfig);
                job.setMongoJob(mongoJob);
                job.setSource(MONGO_SOURCE);
                profileDto.setJob(job);
                break;
            case REDIS:
                RedisJob redisJob = getRedisJob(dataConfig);
                job.setRedisJob(redisJob);
                job.setSource(REDIS_SOURCE);
                profileDto.setJob(job);
                break;
            case MQTT:
                MqttJob mqttJob = getMqttJob(dataConfig);
                job.setMqttJob(mqttJob);
                job.setSource(MQTT_SOURCE);
                profileDto.setJob(job);
                break;
            case MOCK:
                profileDto.setJob(job);
                break;
            default:
        }
        return TriggerProfile.parseJsonStr(GSON.toJson(profileDto));
    }

    @Data
    public static class Job {

        private String id;
        private String groupId;
        private String streamId;
        private String ip;
        private String retry;
        private String source;
        private String sink;
        private String channel;
        private String name;
        private String op;
        private String retryTime;
        private String deliveryTime;
        private String uuid;
        private Integer version;
        private boolean proxySend;
        private String mqClusters;
        private String topicInfo;

        private FileJob fileJob;
        private BinlogJob binlogJob;
        private KafkaJob kafkaJob;
        private PostgreSQLJob postgreSQLJob;
        private OracleJob oracleJob;
        private MongoJob mongoJob;
        private RedisJob redisJob;
        private MqttJob mqttJob;
        private SqlServerJob sqlserverJob;
    }

    @Data
    public static class Manager {

        private String port;
        private String host;
    }

    @Data
    public static class Proxy {

        private String inlongGroupId;
        private String inlongStreamId;
        private Manager manager;
        private Boolean sync;
        private String partitionKey;
    }

}