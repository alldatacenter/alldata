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
        dir.setPattern(fileJobTaskConfig.getPattern());
        fileJob.setDir(dir);
        fileJob.setCollectType(fileJobTaskConfig.getCollectType());
        fileJob.setContentCollectType(fileJobTaskConfig.getContentCollectType());
        fileJob.setDataSeparator(fileJobTaskConfig.getDataSeparator());
        fileJob.setProperties(fileJobTaskConfig.getProperties());
        if (fileJobTaskConfig.getTimeOffset() != null) {
            fileJob.setTimeOffset(fileJobTaskConfig.getTimeOffset());
        }

        if (fileJobTaskConfig.getAdditionalAttr() != null) {
            fileJob.setAddictiveString(fileJobTaskConfig.getAdditionalAttr());
        }

        if (null != fileJobTaskConfig.getLineEndPattern()) {
            FileJob.Line line = new Line();
            line.setEndPattern(fileJobTaskConfig.getLineEndPattern());
        }

        if (null != fileJobTaskConfig.getEnvList()) {
            fileJob.setEnvList(fileJobTaskConfig.getEnvList());
        }

        if (null != fileJobTaskConfig.getMetaFields()) {
            fileJob.setMetaFields(fileJob.getMetaFields());
        }

        if (null != fileJobTaskConfig.getFilterMetaByLabels()) {
            fileJob.setFilterMetaByLabels(fileJobTaskConfig.getFilterMetaByLabels());
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
        job.setSink(DEFAULT_DATAPROXY_SINK);
        job.setVersion(dataConfig.getVersion());
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

        private FileJob fileJob;
        private BinlogJob binlogJob;
        private KafkaJob kafkaJob;
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
    }

}