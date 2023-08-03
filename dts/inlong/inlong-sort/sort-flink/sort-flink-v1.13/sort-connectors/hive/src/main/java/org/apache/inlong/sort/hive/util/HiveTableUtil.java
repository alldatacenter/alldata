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

package org.apache.inlong.sort.hive.util;

import org.apache.inlong.sort.base.sink.PartitionPolicy;
import org.apache.inlong.sort.hive.HiveWriterFactory;
import org.apache.inlong.sort.hive.table.HiveTableInlongFactory;

import org.apache.commons.collections.map.CaseInsensitiveMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.connectors.hive.FlinkHiveException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.table.catalog.hive.client.HiveMetastoreClientFactory;
import org.apache.flink.table.catalog.hive.client.HiveMetastoreClientWrapper;
import org.apache.flink.table.catalog.hive.client.HiveShim;
import org.apache.flink.table.catalog.hive.util.HiveReflectionUtils;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.types.AtomicDataType;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.DecimalType;
import org.apache.flink.table.types.logical.LogicalType;
import org.apache.flink.table.types.logical.LogicalTypeRoot;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.logical.RowType.RowField;
import org.apache.flink.table.types.logical.TimestampType;
import org.apache.flink.table.types.logical.ZonedTimestampType;
import org.apache.flink.table.types.logical.utils.LogicalTypeParser;
import org.apache.flink.types.RowKind;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.IMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.Database;
import org.apache.hadoop.hive.metastore.api.EnvironmentContext;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hadoop.hive.metastore.api.SerDeInfo;
import org.apache.hadoop.hive.metastore.api.StorageDescriptor;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.hadoop.mapred.JobConf;
import org.apache.thrift.TException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.flink.table.types.logical.LogicalTypeRoot.DATE;
import static org.apache.flink.table.types.logical.LogicalTypeRoot.TIMESTAMP_WITHOUT_TIME_ZONE;
import static org.apache.flink.table.types.logical.LogicalTypeRoot.TIMESTAMP_WITH_LOCAL_TIME_ZONE;
import static org.apache.flink.table.types.logical.LogicalTypeRoot.TIMESTAMP_WITH_TIME_ZONE;
import static org.apache.inlong.sort.hive.HiveOptions.SINK_PARTITION_NAME;

/**
 * Utility class for list or create hive table
 */
public class HiveTableUtil {

    private static final Logger LOG = LoggerFactory.getLogger(HiveTableUtil.class);
    private static final String DEFAULT_PARTITION_DATE_FIELD = "[(create)(update)].*[(date)(time)]";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static boolean changeSchema(RowType schema,
            String[] hiveColumns,
            DataType[] hiveTypes,
            String databaseName,
            String tableName,
            String hiveVersion) {
        boolean changed = false;

        Map<String, LogicalType> flinkTypeMap = new HashMap<>();
        for (RowField field : schema.getFields()) {
            // lowercase field name as Oracle field name is uppercase
            flinkTypeMap.put(field.getName().toLowerCase(), field.getType());
        }

        List<String> columnsFromData = new ArrayList<>(flinkTypeMap.keySet());
        List<String> columnsFromHive = Arrays.asList(hiveColumns);
        columnsFromData.removeAll(columnsFromHive);
        // add new field
        for (String fieldName : columnsFromData) {
            boolean result = alterTable(databaseName, tableName, fieldName, flinkTypeMap.get(fieldName), hiveVersion);
            if (result) {
                changed = true;
            }
        }
        // modify field type
        for (int i = 0; i < hiveColumns.length; i++) {
            if (!flinkTypeMap.containsKey(hiveColumns[i])) {
                // hive table has field which source table does not have, ignore it
                // so not support drop field dynamic now
                continue;
            }
            LogicalType flinkType = flinkTypeMap.get(hiveColumns[i]);
            if (hiveTypes[i].getLogicalType().getTypeRoot() != flinkType.getTypeRoot()) {
                // field type changed
                boolean result = alterTable(databaseName, tableName, hiveColumns[i], flinkType, hiveVersion);
                if (result) {
                    changed = true;
                }
            }
        }
        return changed;
    }

    public static boolean alterTable(String databaseName, String tableName, String fieldName, LogicalType type,
            String hiveVersion) {
        HiveConf hiveConf = HiveTableInlongFactory.getHiveConf();
        try (HiveMetastoreClientWrapper client = HiveMetastoreClientFactory.create(hiveConf, hiveVersion)) {
            FieldSchema schema = new FieldSchema(fieldName, flinkType2HiveType(type), "");

            Table table = client.getTable(databaseName, tableName);
            ObjectIdentifier identifier = createObjectIdentifier(databaseName, tableName);
            List<FieldSchema> fieldSchemaList = getTableFields(client, identifier);
            // remove partition keys
            fieldSchemaList.removeAll(table.getPartitionKeys());
            boolean alter = false;
            boolean exist = false;
            for (FieldSchema fieldSchema : fieldSchemaList) {
                if (fieldSchema.getName().equals(fieldName)) {
                    exist = true;
                    if (!flinkType2HiveType(type).equalsIgnoreCase(fieldSchema.getType())) {
                        LOG.info("table {}.{} field {} change type from {} to {}", databaseName, tableName, fieldName,
                                fieldSchema.getType(), flinkType2HiveType(type));
                        fieldSchema.setType(flinkType2HiveType(type));
                        alter = true;
                    }
                    break;
                }
            }
            if (!exist) {
                LOG.info("table {}.{} add new field {} with type {}", databaseName, tableName, fieldName,
                        flinkType2HiveType(type));
                fieldSchemaList.add(schema);
                alter = true;
            }
            if (alter) {
                table.getSd().setCols(fieldSchemaList);
                IMetaStoreClient metaStoreClient = getMetaStoreClient(client);
                EnvironmentContext environmentContext = new EnvironmentContext();
                environmentContext.putToProperties("CASCADE", "true");
                metaStoreClient.alter_table_with_environmentContext(databaseName, tableName, table, environmentContext);
                LOG.info("alter table {}.{} success", databaseName, tableName);
                return true;
            }
        } catch (TException e) {
            throw new CatalogException(String.format("Failed to alter hive table %s.%s", databaseName, tableName), e);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
        return false;
    }

    /**
     * create hive table with default `pt` partition field
     *
     * @param databaseName database name
     * @param tableName table name
     * @param schema flink field type
     * @param partitionPolicy policy of partitioning table
     * @param hiveVersion hive version
     * @param inputFormat the input format of storage descriptor
     * @param outputFormat the output format of storage descriptor
     * @param serializationLib the serialization library of storage descriptor
     */
    public static void createTable(String databaseName, String tableName, RowType schema,
            PartitionPolicy partitionPolicy, String hiveVersion, String inputFormat, String outputFormat,
            String serializationLib) {
        HiveConf hiveConf = HiveTableInlongFactory.getHiveConf();
        try (HiveMetastoreClientWrapper client = HiveMetastoreClientFactory.create(hiveConf, hiveVersion)) {

            List<String> dbs = client.getAllDatabases();
            boolean dbExist = dbs.stream().anyMatch(databaseName::equalsIgnoreCase);
            // create database if not exists
            if (!dbExist) {
                Database defaultDb = client.getDatabase("default");
                Database targetDb = new Database();
                targetDb.setName(databaseName);
                targetDb.setLocationUri(defaultDb.getLocationUri() + "/" + databaseName + ".db");
                client.createDatabase(targetDb);
            }

            String sinkPartitionName = hiveConf.get(SINK_PARTITION_NAME.key(), SINK_PARTITION_NAME.defaultValue());
            FieldSchema defaultPartition = new FieldSchema(sinkPartitionName, "string", "");

            List<FieldSchema> fieldSchemaList = new ArrayList<>();
            for (RowField field : schema.getFields()) {
                if (!field.getName().equals(sinkPartitionName)) {
                    FieldSchema hiveFieldSchema = new FieldSchema(field.getName(), flinkType2HiveType(field.getType()),
                            "");
                    fieldSchemaList.add(hiveFieldSchema);
                }
            }

            Table table = new Table();
            table.setDbName(databaseName);
            table.setTableName(tableName);

            if (PartitionPolicy.NONE != partitionPolicy) {
                table.setPartitionKeys(Collections.singletonList(defaultPartition));
            }

            StorageDescriptor sd = new StorageDescriptor();
            table.setSd(sd);
            sd.setCols(fieldSchemaList);
            sd.setInputFormat(inputFormat);
            sd.setOutputFormat(outputFormat);
            sd.setSerdeInfo(new SerDeInfo());
            sd.getSerdeInfo().setSerializationLib(serializationLib);
            client.createTable(table);
            LOG.info("create table {}.{}", databaseName, tableName);
        } catch (TException e) {
            throw new CatalogException("Failed to create database", e);
        }
    }

    /**
     * Cache hive wrtier factory
     *
     * @param hiveShim hiveShim object
     * @param hiveVersion hive version
     * @param identifier object identifier
     * @return hive writer factory
     */
    public static HiveWriterFactory getWriterFactory(HiveShim hiveShim, String hiveVersion,
            ObjectIdentifier identifier) {
        if (!CacheHolder.getFactoryMap().containsKey(identifier)) {
            HiveConf hiveConf = HiveTableInlongFactory.getHiveConf();
            try (HiveMetastoreClientWrapper client = HiveMetastoreClientFactory.create(hiveConf, hiveVersion)) {
                List<String> tableNames = client.getAllTables(identifier.getDatabaseName());
                LOG.info("table names: {}", Arrays.deepToString(tableNames.toArray()));
                boolean tableExist = tableNames.stream().anyMatch(identifier.getObjectName()::equalsIgnoreCase);
                if (!tableExist) {
                    return null;
                }

                Table table = client.getTable(identifier.getDatabaseName(), identifier.getObjectName());
                StorageDescriptor sd = table.getSd();

                List<FieldSchema> fieldSchemaList = getTableFields(client, identifier);

                List<FieldSchema> partitionSchemas = table.getPartitionKeys();
                String[] partitions = partitionSchemas.stream().map(FieldSchema::getName).toArray(String[]::new);

                TableSchema.Builder builder = new TableSchema.Builder();
                for (FieldSchema fieldSchema : fieldSchemaList) {
                    LogicalType logicalType = LogicalTypeParser.parse(fieldSchema.getType());
                    if (logicalType instanceof TimestampType) {
                        TimestampType timestampType = (TimestampType) logicalType;
                        logicalType = new TimestampType(timestampType.isNullable(), timestampType.getKind(), 9);
                    } else if (logicalType instanceof ZonedTimestampType) {
                        ZonedTimestampType timestampType = (ZonedTimestampType) logicalType;
                        logicalType = new ZonedTimestampType(timestampType.isNullable(), timestampType.getKind(), 9);
                    }
                    builder.field(fieldSchema.getName(), new AtomicDataType(logicalType));
                }
                TableSchema schema = builder.build();

                Class hiveOutputFormatClz = hiveShim.getHiveOutputFormatClass(Class.forName(sd.getOutputFormat()));
                boolean isCompressed = hiveConf.getBoolean(HiveConf.ConfVars.COMPRESSRESULT.varname, false);
                HiveWriterFactory writerFactory = new HiveWriterFactory(new JobConf(hiveConf), hiveOutputFormatClz, sd,
                        schema, partitions, HiveReflectionUtils.getTableMetadata(hiveShim, table), hiveShim,
                        isCompressed, true);
                CacheHolder.getFactoryMap().put(identifier, writerFactory);
            } catch (TException e) {
                throw new CatalogException("Failed to query Hive metaStore", e);
            } catch (ClassNotFoundException e) {
                throw new FlinkHiveException("Failed to get output format class", e);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new FlinkHiveException("Failed to get hive metastore client", e);
            }
        }
        return CacheHolder.getFactoryMap().get(identifier);
    }

    /**
     * get hive metastore client
     *
     * @param client hive metastore client
     * @return hive metastore client
     */
    public static IMetaStoreClient getMetaStoreClient(HiveMetastoreClientWrapper client)
            throws NoSuchFieldException, IllegalAccessException {
        Field clientField = HiveMetastoreClientWrapper.class.getDeclaredField("client");
        clientField.setAccessible(true);
        return (IMetaStoreClient) clientField.get(client);
    }

    /**
     * get hive table fields schema
     *
     * @param client hive metastore client
     * @param identifier hive database and table name
     * @return hive field schema
     */
    public static List<FieldSchema> getTableFields(HiveMetastoreClientWrapper client, ObjectIdentifier identifier)
            throws TException, NoSuchFieldException, IllegalAccessException {
        IMetaStoreClient metaStoreClient = getMetaStoreClient(client);
        return metaStoreClient.getSchema(identifier.getDatabaseName(), identifier.getObjectName());
    }

    /**
     * if raw data has field like create_date or create_time, give it to default partition field.
     * or give first timestamp or date type field value to default partition field
     *
     * @param rawData row data
     * @param schema flink field types
     * @param partitionPolicy partition policy
     * @param partitionField partition field
     * @param timestampPattern timestamp pattern
     * @return default partition value
     */
    public static Object getDefaultPartitionValue(Map<String, Object> rawData, RowType schema,
            PartitionPolicy partitionPolicy, String partitionField, String timestampPattern) {
        if (PartitionPolicy.NONE == partitionPolicy) {
            return null;
        }

        if (PartitionPolicy.PROC_TIME == partitionPolicy) {
            return DateTimeFormatter.ofPattern(timestampPattern).format(LocalDateTime.now());
        }

        String defaultPartitionFieldName = DEFAULT_PARTITION_DATE_FIELD;
        if (PartitionPolicy.ASSIGN_FIELD == partitionPolicy) {
            defaultPartitionFieldName = partitionField;
        }
        Pattern pattern = Pattern.compile(Pattern.quote(defaultPartitionFieldName), Pattern.CASE_INSENSITIVE);
        for (RowField field : schema.getFields()) {
            LogicalTypeRoot type = field.getType().getTypeRoot();
            if (type == TIMESTAMP_WITH_LOCAL_TIME_ZONE || type == TIMESTAMP_WITH_TIME_ZONE
                    || type == TIMESTAMP_WITHOUT_TIME_ZONE || type == DATE) {
                if (pattern.matcher(field.getName()).matches()) {
                    String value = (String) rawData.get(field.getName());
                    return formatDate(value, timestampPattern);
                }
            }
        }
        if (PartitionPolicy.SOURCE_DATE_FIELD == partitionPolicy) {
            // no create or update time field, return proc time
            return DateTimeFormatter.ofPattern(timestampPattern).format(LocalDateTime.now());
        }
        return null;
    }

    public static String formatDate(String dateStr, String toPattern) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        LocalDateTime localDateTime = parseDate(dateStr);
        if (localDateTime == null) {
            return null;
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(toPattern);
        return formatter.format(localDateTime);
    }

    public static LocalDateTime parseDate(String dateStr) {
        if (StringUtils.isBlank(dateStr)) {
            return null;
        }
        ZonedDateTime zonedDateTime = null;
        try {
            zonedDateTime = ZonedDateTime.parse(dateStr,
                    DateTimeFormatter.ofPattern("yyyy-MM-dd['T'][' ']HH:mm:ss[.SSS][XXX][Z]"));
            return LocalDateTime.ofInstant(zonedDateTime.toInstant(), ZoneId.systemDefault());
        } catch (DateTimeParseException ignored) {
            String[] patterns = new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyyMMdd"};
            for (String pattern : patterns) {
                try {
                    return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern(pattern));
                } catch (DateTimeParseException ignored2) {
                }
            }
        }
        return null;
    }

    /**
     * flink type to hive type
     *
     * @param type flink field type
     * @return hive type
     */
    public static String flinkType2HiveType(LogicalType type) {
        switch (type.getTypeRoot()) {
            case INTEGER:
                return "int";
            case BIGINT:
                return "bigint";
            case TINYINT:
                return "tinyint";
            case SMALLINT:
                return "smallint";
            case FLOAT:
                return "float";
            case DOUBLE:
                return "double";
            case DECIMAL:
                DecimalType decimalType = (DecimalType) type;
                return String.format("decimal(%s,%s)", decimalType.getPrecision(), decimalType.getScale());
            case TIMESTAMP_WITH_TIME_ZONE:
            case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
            case TIMESTAMP_WITHOUT_TIME_ZONE:
            case TIME_WITHOUT_TIME_ZONE:
                return "timestamp";
            case DATE:
                return "date";
            case BOOLEAN:
                return "boolean";
            case BINARY:
            case VARBINARY:
                return "binary";
            case ARRAY:
                return "array";
            case CHAR:
            case VARCHAR:
            default:
                return "string";
        }
    }

    /**
     * convert json data to flink generic row data
     *
     * @param record json data
     * @param allColumns hive column names
     * @param allTypes hive column types
     * @param replaceLineBreak if replace line break to blank to avoid data corrupt when hive text table
     * @return generic row data and byte size of the data
     */
    public static Pair<GenericRowData, Integer> getRowData(Map<String, Object> record, String[] allColumns,
            DataType[] allTypes, boolean replaceLineBreak) {
        GenericRowData genericRowData = new GenericRowData(RowKind.INSERT, allColumns.length);
        int byteSize = 0;
        for (int index = 0; index < allColumns.length; index++) {
            String columnName = allColumns[index];
            LogicalType logicalType = allTypes[index].getLogicalType();
            LogicalTypeRoot typeRoot = logicalType.getTypeRoot();
            Object raw = record.get(columnName);
            byteSize += raw == null ? 0 : String.valueOf(raw).getBytes(StandardCharsets.UTF_8).length;
            switch (typeRoot) {
                case BOOLEAN:
                    genericRowData.setField(index, raw != null ? Boolean.parseBoolean(String.valueOf(raw)) : null);
                    break;
                case VARBINARY:
                case BINARY:
                    byte[] bytes = null;
                    if (raw instanceof byte[]) {
                        bytes = (byte[]) raw;
                    } else if (raw instanceof String) {
                        bytes = ((String) raw).getBytes(StandardCharsets.UTF_8);
                    }
                    genericRowData.setField(index, bytes);
                    break;
                case DECIMAL:
                    genericRowData.setField(index, raw != null ? new BigDecimal(String.valueOf(raw)) : null);
                    break;
                case DOUBLE:
                    genericRowData.setField(index, raw != null ? Double.valueOf(String.valueOf(raw)) : null);
                    break;
                case FLOAT:
                    genericRowData.setField(index, raw != null ? Float.valueOf(String.valueOf(raw)) : null);
                    break;
                case INTEGER:
                    genericRowData.setField(index, raw != null ? Integer.valueOf(String.valueOf(raw)) : null);
                    break;
                case BIGINT:
                    genericRowData.setField(index, raw != null ? Long.valueOf(String.valueOf(raw)) : null);
                    break;
                case TINYINT:
                    genericRowData.setField(index, raw != null ? Short.valueOf(String.valueOf(raw)) : null);
                    break;
                case CHAR:
                case VARCHAR:
                    String value = null;
                    if (raw != null) {
                        value = String.valueOf(record.get(columnName));
                        if (replaceLineBreak) {
                            value = value.replaceAll("[\r\n]", " ");
                        }
                    }
                    genericRowData.setField(index, value);
                    break;
                case DATE:
                case TIMESTAMP_WITHOUT_TIME_ZONE:
                case TIMESTAMP_WITH_TIME_ZONE:
                case TIMESTAMP_WITH_LOCAL_TIME_ZONE:
                    if (raw instanceof String) {
                        genericRowData.setField(index, parseDate((String) raw));
                    } else if (raw instanceof Long) {
                        genericRowData.setField(index, new Date((Long) raw));
                    }
                    break;
                default:
                    break;
            }
        }
        return new ImmutablePair<>(genericRowData, byteSize);
    }

    /**
     * Convert json node to hash map.
     * Lowercase the keys of map, because Oracle cdc sends record with uppercase field name, but hive table only
     * supports lowercase field name
     *
     * @param data json node
     * @return list of map data
     */
    public static List<Map<String, Object>> jsonNode2Map(JsonNode data) {
        if (data == null) {
            return new ArrayList<>();
        }
        List<Map<String, Object>> values = new ArrayList<>();
        if (data.isArray()) {
            for (int i = 0; i < data.size(); i++) {
                values.add(jsonObject2Map(data.get(0)));
            }
        } else {
            values.add(jsonObject2Map(data));
        }
        return values;
    }

    /**
     * convert json object, such as JsonNode, JsonObject, to map
     * @param data json object, such as JsonNode, JsonObject
     * @return Map data
     */
    public static Map<String, Object> jsonObject2Map(Object data) {
        CaseInsensitiveMap map = new CaseInsensitiveMap();
        map.putAll(objectMapper.convertValue(data, new TypeReference<Map<String, Object>>() {
        }));
        return map;
    }

    /**
     * convert map to JsonNode
     *
     * @param data map data
     * @return json node
     */
    public static JsonNode object2JsonNode(Map<String, Object> data) {
        try {
            return objectMapper.readTree(objectMapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * create object identifier
     *
     * @param databaseName database name
     * @param tableName table name
     * @return
     */
    public static ObjectIdentifier createObjectIdentifier(String databaseName, String tableName) {
        return ObjectIdentifier.of("default_catalog", databaseName, tableName);
    }
}
