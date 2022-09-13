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

package org.apache.inlong.manager.pojo.sort.util;

import com.google.common.base.Splitter;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.consts.SourceType;
import org.apache.inlong.manager.common.enums.DataSeparator;
import org.apache.inlong.manager.pojo.source.StreamSource;
import org.apache.inlong.manager.pojo.source.kafka.KafkaOffset;
import org.apache.inlong.manager.pojo.source.kafka.KafkaSource;
import org.apache.inlong.manager.pojo.source.mongodb.MongoDBSource;
import org.apache.inlong.manager.pojo.source.mysql.MySQLBinlogSource;
import org.apache.inlong.manager.pojo.source.oracle.OracleSource;
import org.apache.inlong.manager.pojo.source.postgresql.PostgreSQLSource;
import org.apache.inlong.manager.pojo.source.pulsar.PulsarSource;
import org.apache.inlong.manager.pojo.source.sqlserver.SQLServerSource;
import org.apache.inlong.manager.pojo.source.tubemq.TubeMQSource;
import org.apache.inlong.manager.pojo.stream.StreamField;
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
import org.apache.inlong.sort.protocol.node.extract.TubeMQExtractNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.InLongMsgFormat;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.format.RawFormat;

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
        String sourceType = sourceInfo.getSourceType();
        switch (sourceType) {
            case SourceType.MYSQL_BINLOG:
                return createExtractNode((MySQLBinlogSource) sourceInfo);
            case SourceType.KAFKA:
                return createExtractNode((KafkaSource) sourceInfo);
            case SourceType.PULSAR:
                return createExtractNode((PulsarSource) sourceInfo);
            case SourceType.POSTGRESQL:
                return createExtractNode((PostgreSQLSource) sourceInfo);
            case SourceType.ORACLE:
                return createExtractNode((OracleSource) sourceInfo);
            case SourceType.SQLSERVER:
                return createExtractNode((SQLServerSource) sourceInfo);
            case SourceType.MONGODB:
                return createExtractNode((MongoDBSource) sourceInfo);
            case SourceType.TUBEMQ:
                return createExtractNode((TubeMQSource) sourceInfo);
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
        final String database = binlogSource.getDatabaseWhiteList();
        final String primaryKey = binlogSource.getPrimaryKey();
        final String hostName = binlogSource.getHostname();
        final String username = binlogSource.getUser();
        final String password = binlogSource.getPassword();
        final Integer port = binlogSource.getPort();
        Integer serverId = null;
        if (binlogSource.getServerId() != null && binlogSource.getServerId() > 0) {
            serverId = binlogSource.getServerId();
        }
        String tables = binlogSource.getTableWhiteList();
        final List<String> tableNames = Splitter.on(",").splitToList(tables);
        List<FieldInfo> fieldInfos = parseFieldInfos(binlogSource.getFieldList(), binlogSource.getSourceName());
        final String serverTimeZone = binlogSource.getServerTimezone();
        boolean incrementalSnapshotEnabled = true;

        // TODO Needs to be configurable for those parameters
        Map<String, String> properties = parseProperties(binlogSource.getProperties());
        if (binlogSource.isAllMigration()) {
            // Unique properties when migrate all tables in database
            incrementalSnapshotEnabled = false;
            properties.put("migrate-all", "true");
        }
        if (StringUtils.isEmpty(primaryKey)) {
            incrementalSnapshotEnabled = false;
            properties.put("scan.incremental.snapshot.enabled", "false");
        }
        return new MySqlExtractNode(binlogSource.getSourceName(),
                binlogSource.getSourceName(),
                fieldInfos,
                null,
                properties,
                primaryKey,
                tableNames,
                hostName,
                username,
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
        List<FieldInfo> fieldInfos = parseFieldInfos(kafkaSource.getFieldList(), kafkaSource.getSourceName());
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
            case SPECIFIC:
                startupMode = KafkaScanStartupMode.SPECIFIC_OFFSETS;
                break;
            case LATEST:
            default:
                startupMode = KafkaScanStartupMode.LATEST_OFFSET;
        }
        final String primaryKey = kafkaSource.getPrimaryKey();
        String groupId = kafkaSource.getGroupId();
        Map<String, String> properties = parseProperties(kafkaSource.getProperties());
        String partitionOffset = kafkaSource.getPartitionOffsets();
        return new KafkaExtractNode(kafkaSource.getSourceName(),
                kafkaSource.getSourceName(),
                fieldInfos,
                null,
                properties,
                topic,
                bootstrapServers,
                format,
                startupMode,
                primaryKey,
                groupId,
                partitionOffset
        );
    }

    /**
     * Create Pulsar extract node
     *
     * @param pulsarSource Pulsar source info
     * @return Pulsar extract node info
     */
    public static PulsarExtractNode createExtractNode(PulsarSource pulsarSource) {
        List<FieldInfo> fieldInfos = parseFieldInfos(pulsarSource.getFieldList(), pulsarSource.getSourceName());
        String fullTopicName =
                pulsarSource.getTenant() + "/" + pulsarSource.getNamespace() + "/" + pulsarSource.getTopic();

        Format format;
        DataTypeEnum dataType = DataTypeEnum.forName(pulsarSource.getSerializationType());
        switch (dataType) {
            case CSV:
                String separator = DataSeparator
                        .forAscii(Integer.parseInt(pulsarSource.getDataSeparator())).getSeparator();
                format = new CsvFormat(separator);
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
            case RAW:
                format = new RawFormat();
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
        Map<String, String> properties = parseProperties(pulsarSource.getProperties());
        return new PulsarExtractNode(pulsarSource.getSourceName(),
                pulsarSource.getSourceName(),
                fieldInfos,
                null,
                properties,
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
     * @param postgreSQLSource PostgreSQL source info
     * @return PostgreSQL extract node info
     */
    public static PostgresExtractNode createExtractNode(PostgreSQLSource postgreSQLSource) {
        List<FieldInfo> fieldInfos = parseFieldInfos(postgreSQLSource.getFieldList(), postgreSQLSource.getSourceName());
        Map<String, String> properties = parseProperties(postgreSQLSource.getProperties());
        return new PostgresExtractNode(postgreSQLSource.getSourceName(), postgreSQLSource.getSourceName(),
                fieldInfos, null, properties, postgreSQLSource.getPrimaryKey(),
                postgreSQLSource.getTableNameList(), postgreSQLSource.getHostname(),
                postgreSQLSource.getUsername(), postgreSQLSource.getPassword(),
                postgreSQLSource.getDatabase(), postgreSQLSource.getSchema(),
                postgreSQLSource.getPort(), postgreSQLSource.getDecodingPluginName());
    }

    /**
     * Create Oracle extract node
     *
     * @param source Oracle source info
     * @return oracle extract node info
     */
    public static OracleExtractNode createExtractNode(OracleSource source) {
        List<FieldInfo> fieldInfos = parseFieldInfos(source.getFieldList(), source.getSourceName());
        ScanStartUpMode scanStartupMode = StringUtils.isBlank(source.getScanStartupMode())
                ? null : ScanStartUpMode.forName(source.getScanStartupMode());
        Map<String, String> properties = parseProperties(source.getProperties());
        return new OracleExtractNode(
                source.getSourceName(),
                source.getSourceName(),
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
     * Create SQLServer extract node
     *
     * @param source SQLServer source info
     * @return SQLServer extract node info
     */
    public static SqlServerExtractNode createExtractNode(SQLServerSource source) {
        List<FieldInfo> fieldInfos = parseFieldInfos(source.getFieldList(), source.getSourceName());
        Map<String, String> properties = parseProperties(source.getProperties());
        return new SqlServerExtractNode(
                source.getSourceName(),
                source.getSourceName(),
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
        List<FieldInfo> fieldInfos = parseFieldInfos(source.getFieldList(), source.getSourceName());
        Map<String, String> properties = parseProperties(source.getProperties());
        return new MongoExtractNode(
                source.getSourceName(),
                source.getSourceName(),
                fieldInfos,
                null,
                properties,
                source.getCollection(),
                source.getHosts(),
                source.getUsername(),
                source.getPassword(),
                source.getDatabase()
        );
    }

    /**
     * Create TubeMQ extract node
     *
     * @param source TubeMQ source info
     * @return TubeMQ extract node info
     */
    public static TubeMQExtractNode createExtractNode(TubeMQSource source) {
        List<FieldInfo> fieldInfos = parseFieldInfos(source.getFieldList(), source.getSourceName());
        Map<String, String> properties = parseProperties(source.getProperties());
        return new TubeMQExtractNode(
                source.getSourceName(),
                source.getSourceName(),
                fieldInfos,
                null,
                properties,
                source.getMasterRpc(),
                source.getTopic(),
                source.getSerializationType(),
                source.getGroupId(),
                source.getSessionKey(),
                source.getTid()
        );
    }

    /**
     * Parse FieldInfos
     *
     * @param streamFields The stream fields
     * @param nodeId The node id
     * @return FieldInfo list
     */
    private static List<FieldInfo> parseFieldInfos(List<StreamField> streamFields, String nodeId) {
        // Filter constant fields
        return streamFields.stream().filter(s -> s.getFieldValue() == null)
                .map(streamFieldInfo -> FieldInfoUtils.parseStreamFieldInfo(streamFieldInfo, nodeId))
                .collect(Collectors.toList());
    }

    /**
     * Parse properties
     *
     * @param properties The properties with string key and object value
     * @return The properties with string key and string value
     */
    private static Map<String, String> parseProperties(Map<String, Object> properties) {
        return properties.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
    }

}
