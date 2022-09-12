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

package org.apache.inlong.manager.service.sort.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.enums.SourceType;
import org.apache.inlong.manager.common.pojo.source.StreamSource;
import org.apache.inlong.manager.common.pojo.source.kafka.KafkaOffset;
import org.apache.inlong.manager.common.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.common.pojo.source.mongodb.MongoDBSource;
import org.apache.inlong.manager.common.pojo.source.mysql.MySQLBinlogSource;
import org.apache.inlong.manager.common.pojo.source.oracle.OracleSource;
import org.apache.inlong.manager.common.pojo.source.postgres.PostgresSource;
import org.apache.inlong.manager.common.pojo.source.pulsar.PulsarSource;
import org.apache.inlong.manager.common.pojo.source.sqlserver.SqlServerSource;
import org.apache.inlong.manager.common.pojo.stream.StreamField;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.constant.OracleConstant.ScanStartUpMode;
import org.apache.inlong.sort.protocol.enums.KafkaScanStartupMode;
import org.apache.inlong.sort.protocol.enums.PulsarScanStartupMode;
import org.apache.inlong.sort.protocol.node.ExtractNode;
import org.apache.inlong.sort.protocol.node.extract.KafkaExtractNode;
import org.apache.inlong.sort.protocol.node.extract.MongoExtractNode;
import org.apache.inlong.sort.protocol.node.extract.MySqlExtractNode;
import org.apache.inlong.sort.protocol.node.extract.OracleExtractNode;
import org.apache.inlong.sort.protocol.node.extract.PostgresExtractNode;
import org.apache.inlong.sort.protocol.node.extract.PulsarExtractNode;
import org.apache.inlong.sort.protocol.node.extract.SqlServerExtractNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.InLongMsgFormat;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Parse SourceInfo to ExtractNode which sort needed
 */
@Slf4j
public class ExtractNodeUtils {

    /**
     * Create extract nodes from the given sources.
     */
    public static List<ExtractNode> createExtractNodes(List<StreamSource> sourceInfos) {
        if (CollectionUtils.isEmpty(sourceInfos)) {
            return Lists.newArrayList();
        }
        return sourceInfos.stream().map(ExtractNodeUtils::createExtractNode)
                .collect(Collectors.toList());
    }

    public static ExtractNode createExtractNode(StreamSource sourceInfo) {
        SourceType sourceType = SourceType.forType(sourceInfo.getSourceType());
        switch (sourceType) {
            case BINLOG:
                return createExtractNode((MySQLBinlogSource) sourceInfo);
            case KAFKA:
                return createExtractNode((KafkaSource) sourceInfo);
            case PULSAR:
                return createExtractNode((PulsarSource) sourceInfo);
            case POSTGRES:
                return createExtractNode((PostgresSource) sourceInfo);
            case ORACLE:
                return createExtractNode((OracleSource) sourceInfo);
            case SQLSERVER:
                return createExtractNode((SqlServerSource) sourceInfo);
            case MONGODB:
                return createExtractNode((MongoDBSource) sourceInfo);
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported sourceType=%s to create extractNode", sourceType));
        }
    }

    /**
     * Create MySql extract node
     *
     * @param binlogSource MySql binlog source info
     * @return MySql extract node info
     */
    public static MySqlExtractNode createExtractNode(MySQLBinlogSource binlogSource) {
        final String id = binlogSource.getSourceName();
        final String name = binlogSource.getSourceName();
        final String database = binlogSource.getDatabaseWhiteList();
        final String primaryKey = binlogSource.getPrimaryKey();
        final String hostName = binlogSource.getHostname();
        final String userName = binlogSource.getUser();
        final String password = binlogSource.getPassword();
        final Integer port = binlogSource.getPort();
        Integer serverId = null;
        if (binlogSource.getServerId() != null && binlogSource.getServerId() > 0) {
            serverId = binlogSource.getServerId();
        }
        String tables = binlogSource.getTableWhiteList();
        final List<String> tableNames = Splitter.on(",").splitToList(tables);
        final List<StreamField> streamFields = binlogSource.getFieldList();
        final List<FieldInfo> fieldInfos = streamFields.stream()
                .map(streamField -> FieldInfoUtils.parseStreamFieldInfo(streamField, name))
                .collect(Collectors.toList());
        final String serverTimeZone = binlogSource.getServerTimezone();
        boolean incrementalSnapshotEnabled = true;

        // TODO Needs to be configurable for those parameters
        Map<String, String> properties = Maps.newHashMap();
        if (binlogSource.isAllMigration()) {
            // Unique properties when migrate all tables in database
            incrementalSnapshotEnabled = false;
            properties.put("migrate-all", "true");
        }
        properties.put("append-mode", "true");
        if (StringUtils.isEmpty(primaryKey)) {
            incrementalSnapshotEnabled = false;
            properties.put("scan.incremental.snapshot.enabled", "false");
        }
        return new MySqlExtractNode(id,
                name,
                fieldInfos,
                null,
                properties,
                primaryKey,
                tableNames,
                hostName,
                userName,
                password,
                database,
                port,
                serverId,
                incrementalSnapshotEnabled,
                serverTimeZone);
    }

    /**
     * Create Kafka extract node
     *
     * @param kafkaSource Kafka source info
     * @return Kafka extract node info
     */
    public static KafkaExtractNode createExtractNode(KafkaSource kafkaSource) {
        String id = kafkaSource.getSourceName();
        String name = kafkaSource.getSourceName();
        List<StreamField> streamFields = kafkaSource.getFieldList();
        List<FieldInfo> fieldInfos = streamFields.stream()
                .map(streamFieldInfo -> FieldInfoUtils.parseStreamFieldInfo(streamFieldInfo, name))
                .collect(Collectors.toList());
        String topic = kafkaSource.getTopic();
        String bootstrapServers = kafkaSource.getBootstrapServers();
        Format format;
        DataTypeEnum dataType = DataTypeEnum.forName(kafkaSource.getSerializationType());
        switch (dataType) {
            case CSV:
                format = new CsvFormat();
                break;
            case AVRO:
                format = new AvroFormat();
                break;
            case JSON:
                format = new JsonFormat();
                break;
            case CANAL:
                format = new CanalJsonFormat();
                break;
            case DEBEZIUM_JSON:
                format = new DebeziumJsonFormat();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported dataType=%s for kafka source", dataType));
        }
        KafkaOffset kafkaOffset = KafkaOffset.forName(kafkaSource.getAutoOffsetReset());
        KafkaScanStartupMode startupMode;
        switch (kafkaOffset) {
            case EARLIEST:
                startupMode = KafkaScanStartupMode.EARLIEST_OFFSET;
                break;
            case LATEST:
            default:
                startupMode = KafkaScanStartupMode.LATEST_OFFSET;
        }
        final String primaryKey = kafkaSource.getPrimaryKey();
        String groupId = kafkaSource.getGroupId();

        return new KafkaExtractNode(id,
                name,
                fieldInfos,
                null,
                Maps.newHashMap(),
                topic,
                bootstrapServers,
                format,
                startupMode,
                primaryKey,
                groupId);
    }

    /**
     * Create Pulsar extract node
     *
     * @param pulsarSource Pulsar source info
     * @return Pulsar extract node info
     */
    public static PulsarExtractNode createExtractNode(PulsarSource pulsarSource) {
        String id = pulsarSource.getSourceName();
        String name = pulsarSource.getSourceName();
        List<StreamField> streamFields = pulsarSource.getFieldList();
        List<FieldInfo> fieldInfos = streamFields.stream()
                .map(streamFieldInfo -> FieldInfoUtils.parseStreamFieldInfo(streamFieldInfo, name))
                .collect(Collectors.toList());
        String fullTopicName =
                pulsarSource.getTenant() + "/" + pulsarSource.getNamespace() + "/" + pulsarSource.getTopic();

        Format format;
        DataTypeEnum dataType = DataTypeEnum.forName(pulsarSource.getSerializationType());
        switch (dataType) {
            case CSV:
                format = new CsvFormat();
                break;
            case AVRO:
                format = new AvroFormat();
                break;
            case JSON:
                format = new JsonFormat();
                break;
            case CANAL:
                format = new CanalJsonFormat();
                break;
            case DEBEZIUM_JSON:
                format = new DebeziumJsonFormat();
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Unsupported dataType=%s for pulsar source", dataType));
        }
        if (pulsarSource.isInlongComponent()) {
            Format innerFormat = format;
            format = new InLongMsgFormat(innerFormat, false);
        }
        PulsarScanStartupMode startupMode = PulsarScanStartupMode.forName(pulsarSource.getScanStartupMode());
        final String primaryKey = pulsarSource.getPrimaryKey();
        final String serviceUrl = pulsarSource.getServiceUrl();
        final String adminUrl = pulsarSource.getAdminUrl();

        return new PulsarExtractNode(id,
                name,
                fieldInfos,
                null,
                Maps.newHashMap(),
                fullTopicName,
                adminUrl,
                serviceUrl,
                format,
                startupMode.getValue(),
                primaryKey);
    }

    /**
     * Create PostgreSQL extract node
     *
     * @param postgresSource PostgreSQL source info
     * @return PostgreSQL extract node info
     */
    public static PostgresExtractNode createExtractNode(PostgresSource postgresSource) {
        List<StreamField> streamFields = postgresSource.getFieldList();
        String id = postgresSource.getSourceName();
        String name = postgresSource.getSourceName();
        List<FieldInfo> fields = streamFields.stream()
                .map(streamFieldInfo -> FieldInfoUtils.parseStreamFieldInfo(streamFieldInfo, name))
                .collect(Collectors.toList());
        return new PostgresExtractNode(id, name, fields, null, null,
                postgresSource.getPrimaryKey(), postgresSource.getTableNameList(),
                postgresSource.getHostname(), postgresSource.getUsername(),
                postgresSource.getPassword(), postgresSource.getDatabase(),
                postgresSource.getSchema(), postgresSource.getPort(),
                postgresSource.getDecodingPluginName());
    }

    /**
     * Create Oracle extract node
     *
     * @param source Oracle source info
     * @return oracle extract node info
     */
    public static OracleExtractNode createExtractNode(OracleSource source) {
        String name = source.getSourceName();
        List<StreamField> streamFieldInfos = source.getFieldList();
        final List<FieldInfo> fieldInfos = streamFieldInfos.stream()
                .map(streamFieldInfo -> FieldInfoUtils.parseStreamFieldInfo(streamFieldInfo, name))
                .collect(Collectors.toList());

        Map<String, String> properties = Maps.newHashMap();
        ScanStartUpMode scanStartupMode = StringUtils.isBlank(source.getScanStartupMode())
                ? null : ScanStartUpMode.forName(source.getScanStartupMode());

        return new OracleExtractNode(
                name,
                name,
                fieldInfos,
                null,
                properties,
                source.getPrimaryKey(),
                source.getHostname(),
                source.getUsername(),
                source.getPassword(),
                source.getDatabase(),
                source.getSchemaName(),
                source.getTableName(),
                source.getPort(),
                scanStartupMode
        );
    }

    /**
     * Create SqlServer extract node
     *
     * @param source SqlServer source info
     * @return SqlServer extract node info
     */
    public static SqlServerExtractNode createExtractNode(SqlServerSource source) {
        String name = source.getSourceName();
        List<StreamField> streamFields = source.getFieldList();
        List<FieldInfo> fieldInfos = streamFields.stream()
                .map(fieldInfo -> FieldInfoUtils.parseStreamFieldInfo(fieldInfo, name))
                .collect(Collectors.toList());

        Map<String, String> properties = Maps.newHashMap();
        return new SqlServerExtractNode(
                name,
                name,
                fieldInfos,
                null,
                properties,
                source.getPrimaryKey(),
                source.getHostname(),
                source.getPort(),
                source.getUsername(),
                source.getPassword(),
                source.getDatabase(),
                source.getSchemaName(),
                source.getTableName(),
                source.getServerTimezone()
        );
    }

    /**
     * Create MongoDB extract node
     *
     * @param source MongoDB source info
     * @return MongoDB extract node info
     */
    public static MongoExtractNode createExtractNode(MongoDBSource source) {
        String name = source.getSourceName();
        List<StreamField> streamFields = source.getFieldList();
        List<FieldInfo> fieldInfos = streamFields.stream()
                .map(streamFieldInfo -> FieldInfoUtils.parseStreamFieldInfo(streamFieldInfo, name))
                .collect(Collectors.toList());
        Map<String, String> properties = Maps.newHashMap();
        return new MongoExtractNode(
                name,
                name,
                fieldInfos,
                null,
                properties,
                source.getPrimaryKey(),
                source.getCollection(),
                source.getHosts(),
                source.getUsername(),
                source.getPassword(),
                source.getDatabase()
        );
    }

}
