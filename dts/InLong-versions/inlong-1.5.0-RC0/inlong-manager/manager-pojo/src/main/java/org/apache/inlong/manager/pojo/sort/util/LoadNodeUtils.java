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

import com.google.common.collect.Lists;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.common.enums.DataTypeEnum;
import org.apache.inlong.manager.common.consts.SinkType;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.enums.FieldType;
import org.apache.inlong.manager.common.exceptions.BusinessException;
import org.apache.inlong.manager.pojo.sink.SinkField;
import org.apache.inlong.manager.pojo.sink.StreamSink;
import org.apache.inlong.manager.pojo.sink.ck.ClickHouseSink;
import org.apache.inlong.manager.pojo.sink.doris.DorisSink;
import org.apache.inlong.manager.pojo.sink.es.ElasticsearchSink;
import org.apache.inlong.manager.pojo.sink.greenplum.GreenplumSink;
import org.apache.inlong.manager.pojo.sink.hbase.HBaseSink;
import org.apache.inlong.manager.pojo.sink.hdfs.HDFSSink;
import org.apache.inlong.manager.pojo.sink.hive.HivePartitionField;
import org.apache.inlong.manager.pojo.sink.hive.HiveSink;
import org.apache.inlong.manager.pojo.sink.hudi.HudiSink;
import org.apache.inlong.manager.pojo.sink.iceberg.IcebergSink;
import org.apache.inlong.manager.pojo.sink.kafka.KafkaSink;
import org.apache.inlong.manager.pojo.sink.mysql.MySQLSink;
import org.apache.inlong.manager.pojo.sink.oracle.OracleSink;
import org.apache.inlong.manager.pojo.sink.postgresql.PostgreSQLSink;
import org.apache.inlong.manager.pojo.sink.sqlserver.SQLServerSink;
import org.apache.inlong.manager.pojo.sink.starrocks.StarRocksSink;
import org.apache.inlong.manager.pojo.sink.tdsqlpostgresql.TDSQLPostgreSQLSink;
import org.apache.inlong.manager.pojo.stream.StreamField;
import org.apache.inlong.sort.formats.common.StringTypeInfo;
import org.apache.inlong.sort.protocol.FieldInfo;
import org.apache.inlong.sort.protocol.constant.HudiConstant;
import org.apache.inlong.sort.protocol.constant.IcebergConstant.CatalogType;
import org.apache.inlong.sort.protocol.node.LoadNode;
import org.apache.inlong.sort.protocol.node.format.AvroFormat;
import org.apache.inlong.sort.protocol.node.format.CanalJsonFormat;
import org.apache.inlong.sort.protocol.node.format.CsvFormat;
import org.apache.inlong.sort.protocol.node.format.DebeziumJsonFormat;
import org.apache.inlong.sort.protocol.node.format.Format;
import org.apache.inlong.sort.protocol.node.format.JsonFormat;
import org.apache.inlong.sort.protocol.node.format.RawFormat;
import org.apache.inlong.sort.protocol.node.load.ClickHouseLoadNode;
import org.apache.inlong.sort.protocol.node.load.DorisLoadNode;
import org.apache.inlong.sort.protocol.node.load.ElasticsearchLoadNode;
import org.apache.inlong.sort.protocol.node.load.FileSystemLoadNode;
import org.apache.inlong.sort.protocol.node.load.GreenplumLoadNode;
import org.apache.inlong.sort.protocol.node.load.HbaseLoadNode;
import org.apache.inlong.sort.protocol.node.load.HiveLoadNode;
import org.apache.inlong.sort.protocol.node.load.HudiLoadNode;
import org.apache.inlong.sort.protocol.node.load.IcebergLoadNode;
import org.apache.inlong.sort.protocol.node.load.KafkaLoadNode;
import org.apache.inlong.sort.protocol.node.load.MySqlLoadNode;
import org.apache.inlong.sort.protocol.node.load.OracleLoadNode;
import org.apache.inlong.sort.protocol.node.load.PostgresLoadNode;
import org.apache.inlong.sort.protocol.node.load.SqlServerLoadNode;
import org.apache.inlong.sort.protocol.node.load.StarRocksLoadNode;
import org.apache.inlong.sort.protocol.node.load.TDSQLPostgresLoadNode;
import org.apache.inlong.sort.protocol.transformation.ConstantParam;
import org.apache.inlong.sort.protocol.transformation.FieldRelation;
import org.apache.inlong.sort.protocol.transformation.FunctionParam;
import org.apache.inlong.sort.protocol.transformation.StringConstantParam;
import org.apache.inlong.sort.protocol.transformation.function.CustomFunction;

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
    public static List<LoadNode> createLoadNodes(List<StreamSink> streamSinks, Map<String, StreamField> fieldMap) {
        if (CollectionUtils.isEmpty(streamSinks)) {
            return Lists.newArrayList();
        }
        return streamSinks.stream()
                .map(sink -> LoadNodeUtils.createLoadNode(sink, fieldMap))
                .collect(Collectors.toList());
    }

    /**
     * Create load node from the stream sink info.
     */
    public static LoadNode createLoadNode(StreamSink streamSink, Map<String, StreamField> constantFieldMap) {
        List<FieldInfo> fieldInfos = streamSink.getSinkFieldList().stream()
                .map(field -> FieldInfoUtils.parseSinkFieldInfo(field, streamSink.getSinkName()))
                .collect(Collectors.toList());
        List<FieldRelation> fieldRelations = parseSinkFields(streamSink.getSinkFieldList(), constantFieldMap);
        Map<String, String> properties = streamSink.getProperties().entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString()));
        String sinkType = streamSink.getSinkType();
        switch (sinkType) {
            case SinkType.KAFKA:
                return createLoadNode((KafkaSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.HIVE:
                return createLoadNode((HiveSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.HBASE:
                return createLoadNode((HBaseSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.POSTGRESQL:
                return createLoadNode((PostgreSQLSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.CLICKHOUSE:
                return createLoadNode((ClickHouseSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.ICEBERG:
                return createLoadNode((IcebergSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.HUDI:
                return createLoadNode((HudiSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.SQLSERVER:
                return createLoadNode((SQLServerSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.ELASTICSEARCH:
                return createLoadNode((ElasticsearchSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.HDFS:
                return createLoadNode((HDFSSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.GREENPLUM:
                return createLoadNode((GreenplumSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.MYSQL:
                return createLoadNode((MySQLSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.ORACLE:
                return createLoadNode((OracleSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.TDSQLPOSTGRESQL:
                return createLoadNode((TDSQLPostgreSQLSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.DORIS:
                return createLoadNode((DorisSink) streamSink, fieldInfos, fieldRelations, properties);
            case SinkType.STARROCKS:
                return createLoadNode((StarRocksSink) streamSink, fieldInfos, fieldRelations, properties);
            default:
                throw new BusinessException(String.format("Unsupported sinkType=%s to create load node", sinkType));
        }
    }

    /**
     * Create load node of Kafka.
     */
    public static KafkaLoadNode createLoadNode(KafkaSink kafkaSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        Integer sinkParallelism = null;
        if (StringUtils.isNotEmpty(kafkaSink.getPartitionNum())) {
            sinkParallelism = Integer.parseInt(kafkaSink.getPartitionNum());
        }
        DataTypeEnum dataType = DataTypeEnum.forType(kafkaSink.getSerializationType());
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
            case RAW:
                format = new RawFormat();
                break;
            default:
                throw new IllegalArgumentException(String.format("Unsupported dataType=%s for Kafka", dataType));
        }

        return new KafkaLoadNode(
                kafkaSink.getSinkName(),
                kafkaSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                Lists.newArrayList(),
                null,
                kafkaSink.getTopicName(),
                kafkaSink.getBootstrapServers(),
                format,
                sinkParallelism,
                properties,
                kafkaSink.getPrimaryKey());
    }

    /**
     * Create load node of Hive.
     */
    public static HiveLoadNode createLoadNode(HiveSink hiveSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        List<FieldInfo> partitionFields = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(hiveSink.getPartitionFieldList())) {
            partitionFields = hiveSink.getPartitionFieldList().stream()
                    .map(partitionField -> new FieldInfo(partitionField.getFieldName(), hiveSink.getSinkName(),
                            FieldInfoUtils.convertFieldFormat(partitionField.getFieldType(),
                                    partitionField.getFieldFormat())))
                    .collect(Collectors.toList());
        }
        return new HiveLoadNode(
                hiveSink.getSinkName(),
                hiveSink.getSinkName(),
                fieldInfos,
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
                partitionFields);
    }

    /**
     * Create load node of HBase.
     */
    public static HbaseLoadNode createLoadNode(HBaseSink hbaseSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new HbaseLoadNode(
                hbaseSink.getSinkName(),
                hbaseSink.getSinkName(),
                fieldInfos,
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
                hbaseSink.getBufferFlushInterval());
    }

    /**
     * Create load node of PostgreSQL.
     */
    public static PostgresLoadNode createLoadNode(PostgreSQLSink postgreSQLSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new PostgresLoadNode(
                postgreSQLSink.getSinkName(),
                postgreSQLSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                postgreSQLSink.getJdbcUrl(),
                postgreSQLSink.getUsername(),
                postgreSQLSink.getPassword(),
                postgreSQLSink.getDbName() + "." + postgreSQLSink.getTableName(),
                postgreSQLSink.getPrimaryKey());
    }

    /**
     * Create load node of ClickHouse.
     */
    public static ClickHouseLoadNode createLoadNode(ClickHouseSink ckSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new ClickHouseLoadNode(
                ckSink.getSinkName(),
                ckSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                ckSink.getDbName() + "." + ckSink.getTableName(),
                ckSink.getJdbcUrl() + "/" + ckSink.getDbName(),
                ckSink.getUsername(),
                ckSink.getPassword(),
                ckSink.getPrimaryKey());
    }

    /**
     * Create load node of Doris.
     */
    public static DorisLoadNode createLoadNode(DorisSink dorisSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        Format format = null;
        if (dorisSink.getSinkMultipleEnable() != null && dorisSink.getSinkMultipleEnable() && StringUtils.isNotBlank(
                dorisSink.getSinkMultipleFormat())) {
            DataTypeEnum dataType = DataTypeEnum.forType(dorisSink.getSinkMultipleFormat());
            switch (dataType) {
                case CANAL:
                    format = new CanalJsonFormat();
                    break;
                case DEBEZIUM_JSON:
                    format = new DebeziumJsonFormat();
                    break;
                default:
                    throw new IllegalArgumentException(String.format("Unsupported dataType=%s for doris", dataType));
            }
        }
        return new DorisLoadNode(
                dorisSink.getSinkName(),
                dorisSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                dorisSink.getFeNodes(),
                dorisSink.getUsername(),
                dorisSink.getPassword(),
                dorisSink.getTableIdentifier(),
                null,
                dorisSink.getSinkMultipleEnable(),
                format,
                dorisSink.getDatabasePattern(),
                dorisSink.getTablePattern());
    }

    /**
     * Create load node of StarRocks.
     */
    public static StarRocksLoadNode createLoadNode(StarRocksSink starRocksSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        Format format = null;
        if (starRocksSink.getSinkMultipleEnable() != null && starRocksSink.getSinkMultipleEnable()
                && StringUtils.isNotBlank(
                        starRocksSink.getSinkMultipleFormat())) {
            DataTypeEnum dataType = DataTypeEnum.forType(starRocksSink.getSinkMultipleFormat());
            switch (dataType) {
                case CANAL:
                    format = new CanalJsonFormat();
                    break;
                case DEBEZIUM_JSON:
                    format = new DebeziumJsonFormat();
                    break;
                default:
                    throw new IllegalArgumentException(
                            String.format("Unsupported dataType=%s for StarRocks", dataType));
            }
        }
        return new StarRocksLoadNode(
                starRocksSink.getSinkName(),
                starRocksSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                starRocksSink.getJdbcUrl(),
                starRocksSink.getLoadUrl(),
                starRocksSink.getUsername(),
                starRocksSink.getPassword(),
                starRocksSink.getDatabaseName(),
                starRocksSink.getTableName(),
                starRocksSink.getPrimaryKey(),
                starRocksSink.getSinkMultipleEnable(),
                format,
                starRocksSink.getDatabasePattern(),
                starRocksSink.getTablePattern());
    }

    /**
     * Create load node of Iceberg.
     */
    public static IcebergLoadNode createLoadNode(IcebergSink icebergSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        CatalogType catalogType = CatalogType.forName(icebergSink.getCatalogType());
        return new IcebergLoadNode(
                icebergSink.getSinkName(),
                icebergSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                icebergSink.getDbName(),
                icebergSink.getTableName(),
                icebergSink.getPrimaryKey(),
                catalogType,
                icebergSink.getCatalogUri(),
                icebergSink.getWarehouse());
    }

    /**
     * Create load node of Hudi.
     */
    public static HudiLoadNode createLoadNode(HudiSink hudiSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        HudiConstant.CatalogType catalogType = HudiConstant.CatalogType.forName(hudiSink.getCatalogType());

        return new HudiLoadNode(
                hudiSink.getSinkName(),
                hudiSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                hudiSink.getDbName(),
                hudiSink.getTableName(),
                hudiSink.getPrimaryKey(),
                catalogType,
                hudiSink.getCatalogUri(),
                hudiSink.getWarehouse(),
                hudiSink.getExtList(),
                hudiSink.getPartitionKey());
    }

    /**
     * Create load node of SQLServer.
     */
    public static SqlServerLoadNode createLoadNode(SQLServerSink sqlServerSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new SqlServerLoadNode(
                sqlServerSink.getSinkName(),
                sqlServerSink.getSinkName(),
                fieldInfos,
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
                sqlServerSink.getPrimaryKey());
    }

    /**
     * Create Elasticsearch load node
     */
    public static ElasticsearchLoadNode createLoadNode(ElasticsearchSink elasticsearchSink,
            List<FieldInfo> fieldInfos, List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new ElasticsearchLoadNode(
                elasticsearchSink.getSinkName(),
                elasticsearchSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                elasticsearchSink.getIndexName(),
                elasticsearchSink.getHosts(),
                elasticsearchSink.getUsername(),
                elasticsearchSink.getPassword(),
                elasticsearchSink.getDocumentType(),
                elasticsearchSink.getPrimaryKey(),
                elasticsearchSink.getEsVersion());
    }

    /**
     * Create load node of HDFS.
     */
    public static FileSystemLoadNode createLoadNode(HDFSSink hdfsSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        List<FieldInfo> partitionFields = Lists.newArrayList();
        if (CollectionUtils.isNotEmpty(hdfsSink.getPartitionFieldList())) {
            partitionFields = hdfsSink.getPartitionFieldList().stream()
                    .map(partitionField -> new FieldInfo(partitionField.getFieldName(), hdfsSink.getSinkName(),
                            FieldInfoUtils.convertFieldFormat(partitionField.getFieldType(),
                                    partitionField.getFieldFormat())))
                    .collect(Collectors.toList());
        }

        return new FileSystemLoadNode(
                hdfsSink.getSinkName(),
                hdfsSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                Lists.newArrayList(),
                hdfsSink.getDataPath(),
                hdfsSink.getFileFormat(),
                null,
                properties,
                partitionFields,
                hdfsSink.getServerTimeZone());
    }

    /**
     * Create greenplum load node
     */
    public static GreenplumLoadNode createLoadNode(GreenplumSink greenplumSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new GreenplumLoadNode(
                greenplumSink.getSinkName(),
                greenplumSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
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
    public static MySqlLoadNode createLoadNode(MySQLSink mysqlSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new MySqlLoadNode(
                mysqlSink.getSinkName(),
                mysqlSink.getSinkName(),
                fieldInfos,
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
     * Create load node of ORACLE.
     */
    public static OracleLoadNode createLoadNode(OracleSink oracleSink, List<FieldInfo> fieldInfos,
            List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new OracleLoadNode(
                oracleSink.getSinkName(),
                oracleSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                oracleSink.getJdbcUrl(),
                oracleSink.getUsername(),
                oracleSink.getPassword(),
                oracleSink.getTableName(),
                oracleSink.getPrimaryKey());
    }

    /**
     * Create load node of TDSQLPostgreSQL.
     */
    public static TDSQLPostgresLoadNode createLoadNode(TDSQLPostgreSQLSink tdsqlPostgreSQLSink,
            List<FieldInfo> fieldInfos, List<FieldRelation> fieldRelations, Map<String, String> properties) {
        return new TDSQLPostgresLoadNode(
                tdsqlPostgreSQLSink.getSinkName(),
                tdsqlPostgreSQLSink.getSinkName(),
                fieldInfos,
                fieldRelations,
                null,
                null,
                null,
                properties,
                tdsqlPostgreSQLSink.getJdbcUrl(),
                tdsqlPostgreSQLSink.getUsername(),
                tdsqlPostgreSQLSink.getPassword(),
                tdsqlPostgreSQLSink.getSchemaName() + "." + tdsqlPostgreSQLSink.getTableName(),
                tdsqlPostgreSQLSink.getPrimaryKey());
    }

    /**
     * Parse information field of data sink.
     */
    public static List<FieldRelation> parseSinkFields(List<SinkField> fieldList,
            Map<String, StreamField> constantFieldMap) {
        if (CollectionUtils.isEmpty(fieldList)) {
            return Lists.newArrayList();
        }
        return fieldList.stream()
                .filter(sinkField -> StringUtils.isNotEmpty(sinkField.getSourceFieldName()))
                .map(field -> {
                    FieldInfo outputField = new FieldInfo(field.getFieldName(),
                            FieldInfoUtils.convertFieldFormat(field.getFieldType(), field.getFieldFormat()));
                    FunctionParam inputField;
                    String fieldKey = String.format("%s-%s", field.getOriginNodeName(), field.getSourceFieldName());
                    StreamField constantField = constantFieldMap.get(fieldKey);
                    if (constantField != null) {
                        if (outputField.getFormatInfo() != null
                                && outputField.getFormatInfo().getTypeInfo() == StringTypeInfo.INSTANCE) {
                            inputField = new StringConstantParam(constantField.getFieldValue());
                        } else {
                            inputField = new ConstantParam(constantField.getFieldValue());
                        }
                    } else if (FieldType.FUNCTION.name().equalsIgnoreCase(field.getSourceFieldType())) {
                        inputField = new CustomFunction(field.getSourceFieldName());
                    } else {
                        inputField = new FieldInfo(field.getSourceFieldName(), field.getOriginNodeName(),
                                FieldInfoUtils.convertFieldFormat(field.getSourceFieldType()));
                    }
                    return new FieldRelation(inputField, outputField);
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
