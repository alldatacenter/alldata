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

/**
 * Utils for create sink info, such as kafka sink, clickhouse sink, etc.
 */
public class SinkInfoUtils {

    private static final String DATA_FORMAT = "yyyyMMddHH";
    private static final String TIME_FORMAT = "HHmmss";
    private static final String DATA_TIME_FORMAT = "yyyyMMddHHmmss";

    /*
     * Create sink info for DataFlowInfo.
     */
    /*public static SinkInfo createSinkInfo(StreamSource streamSource, StreamSink streamSink,
            List<FieldInfo> sinkFields) {
        String sinkType = streamSink.getSinkType();
        SinkInfo sinkInfo;
        switch (SinkType.forType(sinkType)) {
            case HIVE:
                sinkInfo = createHiveSinkInfo((HiveSink) streamSink, sinkFields);
                break;
            case KAFKA:
                sinkInfo = createKafkaSinkInfo(streamSource, (KafkaSink) streamSink, sinkFields);
                break;
            case ICEBERG:
                sinkInfo = createIcebergSinkInfo((IcebergSink) streamSink, sinkFields);
                break;
            case CLICKHOUSE:
                sinkInfo = createClickhouseSinkInfo((ClickHouseSink) streamSink, sinkFields);
                break;
            case HBASE:
                sinkInfo = createHbaseSinkInfo((HBaseSink) streamSink, sinkFields);
                break;
            case ELASTICSEARCH:
                sinkInfo = createEsSinkInfo((ElasticsearchSink) streamSink, sinkFields);
                break;
            default:
                throw new BusinessException(String.format("Unsupported SinkType {%s}", sinkType));
        }
        return sinkInfo;
    }*/

    /*private static ClickHouseSinkInfo createClickhouseSinkInfo(ClickHouseSink ckSink, List<FieldInfo> sinkFields) {
        if (StringUtils.isEmpty(ckSink.getJdbcUrl())) {
            throw new BusinessException(String.format("ClickHouse={%s} jdbc url cannot be empty", ckSink));
        } else if (CollectionUtils.isEmpty(ckSink.getFieldList())) {
            throw new BusinessException(String.format("ClickHouse={%s} fields cannot be empty", ckSink));
        } else if (StringUtils.isEmpty(ckSink.getTableName())) {
            throw new BusinessException(String.format("ClickHouse={%s} table name cannot be empty", ckSink));
        } else if (StringUtils.isEmpty(ckSink.getDbName())) {
            throw new BusinessException(String.format("ClickHouse={%s} database name cannot be empty", ckSink));
        }

        Integer isDistributed = ckSink.getIsDistributed();
        if (isDistributed == null) {
            throw new BusinessException(String.format("ClickHouse={%s} isDistributed cannot be null", ckSink));
        }

        // Default partition strategy is RANDOM
        ClickHouseSinkInfo.PartitionStrategy partitionStrategy = PartitionStrategy.RANDOM;
        boolean distributedTable = isDistributed == 1;
        if (distributedTable) {
            if (PartitionStrategy.BALANCE.name().equalsIgnoreCase(ckSink.getPartitionStrategy())) {
                partitionStrategy = PartitionStrategy.BALANCE;
            } else if (PartitionStrategy.HASH.name().equalsIgnoreCase(ckSink.getPartitionStrategy())) {
                partitionStrategy = PartitionStrategy.HASH;
            }
        }

        // TODO Add keyFieldNames instead of `new String[0]`
        return new ClickHouseSinkInfo(ckSink.getJdbcUrl(), ckSink.getDbName(),
                ckSink.getTableName(), ckSink.getUsername(), ckSink.getPassword(),
                distributedTable, partitionStrategy, ckSink.getPartitionFields(),
                sinkFields.toArray(new FieldInfo[0]), new String[0],
                ckSink.getFlushInterval(), ckSink.getFlushRecord(),
                ckSink.getRetryTimes());
    }*/

    /*// TODO Need set more configs for IcebergSinkInfo
    private static IcebergSinkInfo createIcebergSinkInfo(IcebergSink icebergSink, List<FieldInfo> sinkFields) {
        if (StringUtils.isEmpty(icebergSink.getDataPath())) {
            throw new BusinessException(String.format("Iceberg={%s} data path cannot be empty", icebergSink));
        }

        return new IcebergSinkInfo(sinkFields.toArray(new FieldInfo[0]), icebergSink.getDataPath());
    }*/

    /*private static KafkaSinkInfo createKafkaSinkInfo(StreamSource streamSource, KafkaSink kafkaSink,
            List<FieldInfo> sinkFields) {
        String addressUrl = kafkaSink.getBootstrapServers();
        String topicName = kafkaSink.getTopicName();
        SerializationInfo serializationInfo = SerializationUtils.createSerialInfo(streamSource, kafkaSink);
        return new KafkaSinkInfo(sinkFields.toArray(new FieldInfo[0]), addressUrl, topicName, serializationInfo);
    }*/

    /*
     * Create Hive sink info.
     */
    /*private static HiveSinkInfo createHiveSinkInfo(HiveSink hiveInfo, List<FieldInfo> sinkFields) {
        if (hiveInfo.getJdbcUrl() == null) {
            throw new BusinessException(String.format("HiveSink={%s} server url cannot be empty", hiveInfo));
        }
        if (CollectionUtils.isEmpty(hiveInfo.getFieldList())) {
            throw new BusinessException(String.format("HiveSink={%s} fields cannot be empty", hiveInfo));
        }
        // Use the field separator in Hive, the default is TextFile
        Character separator = (char) Integer.parseInt(hiveInfo.getDataSeparator());
        HiveFileFormat fileFormat;
        FileFormat format = FileFormat.forName(hiveInfo.getFileFormat());

        if (format == FileFormat.ORCFile) {
            fileFormat = new HiveSinkInfo.OrcFileFormat(1000);
        } else if (format == FileFormat.SequenceFile) {
            fileFormat = new HiveSinkInfo.SequenceFileFormat(separator, 100);
        } else if (format == FileFormat.Parquet) {
            fileFormat = new HiveSinkInfo.ParquetFileFormat();
        } else {
            fileFormat = new HiveSinkInfo.TextFileFormat(separator);
        }

        // Handle hive partition list
        List<HivePartitionInfo> partitionList = new ArrayList<>();
        List<HivePartitionField> partitionFieldList = hiveInfo.getPartitionFieldList();
        if (CollectionUtils.isNotEmpty(partitionFieldList)) {
            SinkInfoUtils.checkPartitionField(hiveInfo.getFieldList(), partitionFieldList);
            partitionList = partitionFieldList.stream().map(s -> {
                HivePartitionInfo partition;
                String fieldFormat = s.getFieldFormat();
                switch (FieldType.forName(s.getFieldType())) {
                    case TIMESTAMP:
                        fieldFormat = StringUtils.isNotBlank(fieldFormat) ? fieldFormat : DATA_TIME_FORMAT;
                        partition = new HiveTimePartitionInfo(s.getFieldName(), fieldFormat);
                        break;
                    case DATE:
                        fieldFormat = StringUtils.isNotBlank(fieldFormat) ? fieldFormat : DATA_FORMAT;
                        partition = new HiveTimePartitionInfo(s.getFieldName(), fieldFormat);
                        break;
                    default:
                        partition = new HiveFieldPartitionInfo(s.getFieldName());
                }
                return partition;
            }).collect(Collectors.toList());
        }

        // dataPath = dataPath + / + tableName
        StringBuilder dataPathBuilder = new StringBuilder();
        String dataPath = hiveInfo.getDataPath();
        if (!dataPath.endsWith("/")) {
            dataPathBuilder.append(dataPath).append("/");
        }
        dataPath = dataPathBuilder.append(hiveInfo.getTableName()).toString();

        return new HiveSinkInfo(sinkFields.toArray(new FieldInfo[0]), hiveInfo.getJdbcUrl(),
                hiveInfo.getDbName(), hiveInfo.getTableName(), hiveInfo.getUsername(), hiveInfo.getPassword(),
                dataPath, partitionList.toArray(new HiveSinkInfo.HivePartitionInfo[0]), fileFormat);
    }*/

    /*
     * Check the validation of Hive partition field.
     */
    /*public static void checkPartitionField(List<SinkField> fieldList, List<HivePartitionField> partitionList) {
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
    }*/

    /*
     * Creat HBase sink info.
     */
    /*private static HbaseSinkInfo createHbaseSinkInfo(HBaseSink hbaseSink, List<FieldInfo> sinkFields) {
        if (StringUtils.isEmpty(hbaseSink.getZkQuorum())) {
            throw new BusinessException(String.format("HBase={%s} zookeeper quorum url cannot be empty", hbaseSink));
        } else if (StringUtils.isEmpty(hbaseSink.getZkNodeParent())) {
            throw new BusinessException(String.format("HBase={%s} zookeeper node cannot be empty", hbaseSink));
        } else if (StringUtils.isEmpty(hbaseSink.getTableName())) {
            throw new BusinessException(String.format("HBase={%s} table name cannot be empty", hbaseSink));
        }

        return new HbaseSinkInfo(sinkFields.toArray(new FieldInfo[0]), hbaseSink.getZkQuorum(),
                hbaseSink.getZkNodeParent(), hbaseSink.getNamespace(), hbaseSink.getTableName(),
                hbaseSink.getBufferFlushMaxSize(), hbaseSink.getBufferFlushMaxSize(),
                hbaseSink.getBufferFlushInterval());

    }*/

    /*
     * Creat Elasticsearch sink info.
     */
    /*private static ElasticsearchSinkInfo createEsSinkInfo(ElasticsearchSink esSink, List<FieldInfo> sinkFields) {
        if (StringUtils.isEmpty(esSink.getHost())) {
            throw new BusinessException(String.format("es={%s} host cannot be empty", esSink));
        } else if (StringUtils.isEmpty(esSink.getIndexName())) {
            throw new BusinessException(String.format("es={%s} indexName cannot be empty", esSink));
        }

        return new ElasticsearchSinkInfo(esSink.getHost(), esSink.getPort(),
                esSink.getIndexName(), esSink.getUsername(), esSink.getPassword(),
                sinkFields.toArray(new FieldInfo[0]), new String[0],
                esSink.getFlushInterval(), esSink.getFlushRecord(),
                esSink.getRetryTimes());
    }*/

}
