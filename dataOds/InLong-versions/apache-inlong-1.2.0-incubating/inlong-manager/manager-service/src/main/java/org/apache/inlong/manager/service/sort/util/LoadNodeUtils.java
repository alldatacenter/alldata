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

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.SinkType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.common.pojo.sink.SinkField;
import org.apache.inlong.manager.common.pojo.sink.StreamSink;
import org.apache.inlong.manager.common.pojo.sink.ck.ClickHouseSink;
import org.apache.inlong.manager.common.pojo.sink.es.ElasticsearchSink;
import org.apache.inlong.manager.common.pojo.sink.greenplum.GreenplumSink;
import org.apache.inlong.manager.common.pojo.sink.hbase.HBaseSink;
import org.apache.inlong.manager.common.pojo.sink.hdfs.HdfsSink;
import org.apache.inlong.manager.common.pojo.sink.hive.HivePartitionField;
import org.apache.inlong.manager.common.pojo.sink.hive.HiveSink;
import org.apache.inlong.manager.common.pojo.sink.iceberg.IcebergSink;
import org.apache.inlong.manager.common.pojo.sink.kafka.KafkaSink;
import org.apache.inlong.manager.common.pojo.sink.mysql.MySQLSink;
import org.apache.inlong.manager.common.pojo.sink.postgres.PostgresSink;
import org.apache.inlong.manager.common.pojo.sink.sqlserver.SqlServerSink;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.constant.IcebergConstant.CatalogType;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.load.ClickHouseLoadNode;
import org.apache.inlong.sort.protocol.node.load.ElasticsearchLoadNode;
import org.apache.inlong.sort.protocol.node.load.FileSystemLoadNode;
import org.apache.inlong.sort.protocol.node.load.GreenplumLoadNode;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.node.load.HiveLoadNode;
import org.apache.inlong.sort.protocol.node.load.IcebergLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.node.load.MySqlLoadNode;
import org.apache.inlong.sort.protocol.node.load.PostgresLoadNode;
import org.apache.inlong.sort.protocol.node.load.SqlServerLoadNode;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Util for load node info.
 */
public class LoadNodeUtils {

    /**
     * Create nodes of data load.
     */
    public static List<LoadNode> createLoadNodes(List<StreamSink> streamSinks) {
        if (CollectionUtils.isEmpty(streamSinks)) {
            return Lists.newArrayList();
        }
        return streamSinks.stream().map(LoadNodeUtils::createLoadNode).collect(Collectors.toList());
    }

    /**
     * Create load node from the stream sink info.
     */
    public static LoadNode createLoadNode(StreamSink streamSink) {
        SinkType sinkType = SinkType.forType(streamSink.getSinkType());
        switch (sinkType) {
            case KAFKA:
                return createLoadNode((KafkaSink) streamSink);
            case HIVE:
                return createLoadNode((HiveSink) streamSink);
            case HBASE:
                return createLoadNode((HBaseSink) streamSink);
            case POSTGRES:
                return createLoadNode((PostgresSink) streamSink);
            case CLICKHOUSE:
                return createLoadNode((ClickHouseSink) streamSink);
            case ICEBERG:
                return createLoadNode((IcebergSink) streamSink);
            case SQLSERVER:
                return createLoadNode((SqlServerSink) streamSink);
            case ELASTICSEARCH:
                return createLoadNode((ElasticsearchSink) streamSink);
            case HDFS:
                return createLoadNode((HdfsSink) streamSink);
            case GREENPLUM:
                return createLoadNode((GreenplumSink) streamSink);
            case MYSQL:
                return createLoadNode((MySQLSink) streamSink);
            default:
                throw new BusinessException(String.format("Unsupported sinkType=%s to create load node", sinkType));
        }
    }

    /**
     * Create load node of Kafka.
     */
    public static KafkaLoadNode createLoadNode(KafkaSink kafkaSink) {
        String id = kafkaSink.getSinkName();
        String name = kafkaSink.getSinkName();
        List<SinkField> fieldList = kafkaSink.getSinkFieldList();
        List<FieldInfo> fieldInfos = fieldList.stream()
                .map(field -> FieldInfoUtils.parseSinkFieldInfo(field, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);
        Map<String, String> properties = kafkaSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        Integer sinkParallelism = null;
        if (StringUtils.isNotEmpty(kafkaSink.getPartitionNum())) {
            sinkParallelism = Integer.parseInt(kafkaSink.getPartitionNum());
        }
        DataTypeEnum dataType = DataTypeEnum.forName(kafkaSink.getSerializationType());
        Format format;
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
                throw new IllegalArgumentException(String.format("Unsupported dataType=%s for Kafka", dataType));
        }

        return new KafkaLoadNode(
                id,
                name,
                fieldInfos,
                fieldRelations,
                Lists.newArrayList(),
                null,
                kafkaSink.getTopicName(),
                kafkaSink.getBootstrapServers(),
                format,
                sinkParallelism,
                properties,
                kafkaSink.getPrimaryKey()
        );
    }

    /**
     * Create load node of Hive.
     */
    public static HiveLoadNode createLoadNode(HiveSink hiveSink) {
        String id = hiveSink.getSinkName();
        String name = hiveSink.getSinkName();
        List<SinkField> fieldList = hiveSink.getSinkFieldList();
        List<FieldInfo> fields = fieldList.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);
        Map<String, String> properties = hiveSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        List<FieldInfo> partitionFields = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(hiveSink.getPartitionFieldList())) {
            partitionFields = hiveSink.getPartitionFieldList().stream()
                    .map(partitionField -> new FieldInfo(partitionField.getFieldName(), name,
                            FieldInfoUtils.convertFieldFormat(partitionField.getFieldType(),
                                    partitionField.getFieldFormat()))).collect(Collectors.toList());
        }
        return new HiveLoadNode(
                id,
                name,
                fields,
                fieldRelations,
                Lists.newArrayList(),
                null,
                null,
                properties,
                null,
                hiveSink.getDbName(),
                hiveSink.getTableName(),
                hiveSink.getHiveConfDir(),
                hiveSink.getHiveVersion(),
                null,
                partitionFields
        );
    }

    /**
     * Create load node of HBase.
     */
    public static HbaseLoadNode createLoadNode(HBaseSink hbaseSink) {
        String id = hbaseSink.getSinkName();
        String name = hbaseSink.getSinkName();
        List<SinkField> fieldList = hbaseSink.getSinkFieldList();
        List<FieldInfo> fields = fieldList.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);
        Map<String, String> properties = hbaseSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        return new HbaseLoadNode(
                id,
                name,
                fields,
                fieldRelations,
                Lists.newArrayList(),
                null,
                null,
                properties,
                hbaseSink.getTableName(),
                hbaseSink.getNamespace(),
                hbaseSink.getZkQuorum(),
                hbaseSink.getRowKey(),
                hbaseSink.getBufferFlushMaxSize(),
                hbaseSink.getZkNodeParent(),
                hbaseSink.getBufferFlushMaxRows(),
                hbaseSink.getBufferFlushInterval()
        );
    }

    /**
     * Create load node of PostgreSQL.
     */
    public static PostgresLoadNode createLoadNode(PostgresSink postgresSink) {
        List<SinkField> fieldList = postgresSink.getSinkFieldList();
        String name = postgresSink.getSinkName();
        List<FieldInfo> fields = fieldList.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);

        return new PostgresLoadNode(
                name,
                name,
                fields,
                fieldRelations,
                null,
                null,
                1,
                null,
                postgresSink.getJdbcUrl(),
                postgresSink.getUsername(),
                postgresSink.getPassword(),
                postgresSink.getDbName() + "." + postgresSink.getTableName(),
                postgresSink.getPrimaryKey()
        );
    }

    /**
     * Create load node of ClickHouse.
     */
    public static ClickHouseLoadNode createLoadNode(ClickHouseSink ckSink) {
        List<SinkField> sinkFields = ckSink.getSinkFieldList();
        String name = ckSink.getSinkName();
        List<FieldInfo> fields = sinkFields.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(sinkFields, name);

        return new ClickHouseLoadNode(
                name,
                name,
                fields,
                fieldRelations,
                null,
                null,
                1,
                null,
                ckSink.getTableName(),
                ckSink.getJdbcUrl() + "/" + ckSink.getDbName(),
                ckSink.getUsername(),
                ckSink.getPassword()
        );
    }

    /**
     * Create load node of Iceberg.
     */
    public static IcebergLoadNode createLoadNode(IcebergSink icebergSink) {
        String id = icebergSink.getSinkName();
        String name = icebergSink.getSinkName();
        CatalogType catalogType = CatalogType.forName(icebergSink.getCatalogType());
        List<SinkField> sinkFields = icebergSink.getSinkFieldList();
        List<FieldInfo> fields = sinkFields.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelationShips = parseSinkFields(sinkFields, name);
        Map<String, String> properties = icebergSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

        return new IcebergLoadNode(
                id,
                name,
                fields,
                fieldRelationShips,
                null,
                null,
                1,
                properties,
                icebergSink.getDbName(),
                icebergSink.getTableName(),
                icebergSink.getPrimaryKey(),
                catalogType,
                icebergSink.getCatalogUri(),
                icebergSink.getWarehouse()
        );
    }

    /**
     * Create load node of SqlServer.
     */
    public static SqlServerLoadNode createLoadNode(SqlServerSink sqlServerSink) {
        final String id = sqlServerSink.getSinkName();
        final String name = sqlServerSink.getSinkName();
        final List<SinkField> fieldList = sqlServerSink.getSinkFieldList();
        List<FieldInfo> fields = fieldList.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);
        Map<String, String> properties = sqlServerSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

        return new SqlServerLoadNode(
                id,
                name,
                fields,
                fieldRelations,
                null,
                null,
                null,
                properties,
                sqlServerSink.getJdbcUrl(),
                sqlServerSink.getUsername(),
                sqlServerSink.getPassword(),
                sqlServerSink.getSchemaName(),
                sqlServerSink.getTableName(),
                sqlServerSink.getPrimaryKey()
        );
    }

    /**
     * Create Elasticsearch load node
     */
    public static ElasticsearchLoadNode createLoadNode(ElasticsearchSink elasticsearchSink) {
        final String id = elasticsearchSink.getSinkName();
        final String name = elasticsearchSink.getSinkName();
        final List<SinkField> fieldList = elasticsearchSink.getSinkFieldList();
        List<FieldInfo> fields = fieldList.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);
        Map<String, String> properties = elasticsearchSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

        return new ElasticsearchLoadNode(
                id,
                name,
                fields,
                fieldRelations,
                null,
                null,
                null,
                properties,
                elasticsearchSink.getIndexName(),
                elasticsearchSink.getHost(),
                elasticsearchSink.getUsername(),
                elasticsearchSink.getPassword(),
                elasticsearchSink.getDocumentType(),
                elasticsearchSink.getPrimaryKey(),
                elasticsearchSink.getVersion()
        );
    }

    /**
     * Create load node of HDFS.
     */
    public static FileSystemLoadNode createLoadNode(HdfsSink hdfsSink) {
        String id = hdfsSink.getSinkName();
        String name = hdfsSink.getSinkName();
        List<SinkField> fieldList = hdfsSink.getSinkFieldList();
        List<FieldInfo> fields = fieldList.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);
        Map<String, String> properties = hdfsSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        List<FieldInfo> partitionFields = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(hdfsSink.getPartitionFieldList())) {
            partitionFields = hdfsSink.getPartitionFieldList().stream()
                    .map(partitionField -> new FieldInfo(partitionField.getFieldName(), name,
                            FieldInfoUtils.convertFieldFormat(partitionField.getFieldType(),
                                    partitionField.getFieldFormat())))
                    .collect(Collectors.toList());
        }

        return new FileSystemLoadNode(
                id,
                name,
                fields,
                fieldRelations,
                Lists.newArrayList(),
                hdfsSink.getDataPath(),
                hdfsSink.getFileFormat(),
                null,
                properties,
                partitionFields,
                hdfsSink.getServerTimeZone()
        );
    }

    /**
     * Create greenplum load node
     */
    public static GreenplumLoadNode createLoadNode(GreenplumSink greenplumSink) {
        String id = greenplumSink.getSinkName();
        String name = greenplumSink.getSinkName();
        List<SinkField> fieldList = greenplumSink.getSinkFieldList();
        List<FieldInfo> fields = fieldList.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);
        Map<String, String> properties = greenplumSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

        return new GreenplumLoadNode(
                id,
                name,
                fields,
                fieldRelations,
                null,
                null,
                1,
                properties,
                greenplumSink.getJdbcUrl(),
                greenplumSink.getUsername(),
                greenplumSink.getPassword(),
                greenplumSink.getTableName(),
                greenplumSink.getPrimaryKey());
    }

    /**
     * Create load node of MySQL.
     */
    public static MySqlLoadNode createLoadNode(MySQLSink mysqlSink) {
        String id = mysqlSink.getSinkName();
        String name = mysqlSink.getSinkName();
        List<SinkField> fieldList = mysqlSink.getSinkFieldList();
        List<FieldInfo> fields = fieldList.stream()
                .map(sinkField -> FieldInfoUtils.parseSinkFieldInfo(sinkField, name))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(fieldList, name);
        Map<String, String> properties = mysqlSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));

        return new MySqlLoadNode(
                id,
                name,
                fields,
                fieldRelations,
                Lists.newArrayList(),
                null,
                null,
                properties,
                mysqlSink.getJdbcUrl(),
                mysqlSink.getUsername(),
                mysqlSink.getPassword(),
                mysqlSink.getTableName(),
                mysqlSink.getPrimaryKey());
    }

    /**
     * Parse information field of data sink.
     */
    public static List<FieldRelation> parseSinkFields(List<SinkField> fieldList, String sinkName) {
        if (CollectionUtils.isEmpty(fieldList)) {
            return Lists.newArrayList();
        }
        return fieldList.stream()
                .filter(sinkField -> StringUtils.isNotEmpty(sinkField.getSourceFieldName()))
                .map(field -> {
                    String fieldName = field.getFieldName();
                    String fieldType = field.getFieldType();
                    String fieldFormat = field.getFieldFormat();
                    FieldInfo sinkField = new FieldInfo(fieldName, sinkName,
                            FieldInfoUtils.convertFieldFormat(fieldType, fieldFormat));
                    String sourceFieldName = field.getSourceFieldName();
                    String sourceFieldType = field.getSourceFieldType();
                    FieldInfo sourceField = new FieldInfo(sourceFieldName, sinkName,
                            FieldInfoUtils.convertFieldFormat(sourceFieldType));
                    return new FieldRelation(sourceField, sinkField);
                }).collect(Collectors.toList());
    }

    /**
     * Check the validation of Hive partition field.
     */
    public static void checkPartitionField(List<SinkField> fieldList, List<HivePartitionField> partitionList) {
        if (CollectionUtils.isEmpty(partitionList)) {
            return;
        }
        if (CollectionUtils.isEmpty(fieldList)) {
            throw new BusinessException(ErrorCodeEnum.SINK_FIELD_LIST_IS_EMPTY);
        }

        Map<String, SinkField> sinkFieldMap = new HashMap<>(fieldList.size());
        fieldList.forEach(field -> sinkFieldMap.put(field.getFieldName(), field));

        for (HivePartitionField partitionField : partitionList) {
            String fieldName = partitionField.getFieldName();
            if (StringUtils.isBlank(fieldName)) {
                throw new BusinessException(ErrorCodeEnum.PARTITION_FIELD_NAME_IS_EMPTY);
            }

            SinkField sinkField = sinkFieldMap.get(fieldName);
            if (sinkField == null) {
                throw new BusinessException(
                        String.format(ErrorCodeEnum.PARTITION_FIELD_NOT_FOUND.getMessage(), fieldName));
            }
            if (StringUtils.isBlank(sinkField.getSourceFieldName())) {
                throw new BusinessException(
                        String.format(ErrorCodeEnum.PARTITION_FIELD_NO_SOURCE_FIELD.getMessage(), fieldName));
            }
        }
    }

}
