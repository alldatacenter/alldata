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

package org.apache.inlong.sort.hive;

import org.apache.inlong.sort.base.format.DynamicSchemaFormatFactory;
import org.apache.inlong.sort.base.format.JsonDynamicSchemaFormat;
import org.apache.inlong.sort.base.sink.PartitionPolicy;
import org.apache.inlong.sort.hive.util.CacheHolder;
import org.apache.inlong.sort.hive.util.HiveTableUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.JsonNode;
import org.apache.flink.table.catalog.ObjectIdentifier;
import org.apache.flink.table.catalog.hive.client.HiveShim;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.util.DataFormatConverters;
import org.apache.flink.table.filesystem.RowDataPartitionComputer;
import org.apache.flink.table.functions.hive.conversion.HiveInspectors;
import org.apache.flink.table.functions.hive.conversion.HiveObjectConversion;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.table.types.utils.TypeConversions;
import org.apache.hadoop.hive.serde2.objectinspector.ObjectInspector;
import org.apache.hadoop.mapred.JobConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_DATABASE_PATTERN;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_ENABLE;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_FORMAT;
import static org.apache.inlong.sort.base.Constants.SINK_MULTIPLE_TABLE_PATTERN;

/**
 * A {@link RowDataPartitionComputer} that converts Flink objects to Hive objects before computing
 * the partition value strings.
 */
public class HiveRowDataPartitionComputer extends RowDataPartitionComputer {

    private static final Logger LOG = LoggerFactory.getLogger(HiveRowDataPartitionComputer.class);

    private final DataFormatConverters.DataFormatConverter[] partitionConverters;
    private final HiveObjectConversion[] hiveObjectConversions;

    private transient JsonDynamicSchemaFormat jsonFormat;
    private final boolean sinkMultipleEnable;
    private final String sinkMultipleFormat;
    private final String databasePattern;
    private final String tablePattern;
    private final HiveShim hiveShim;

    private final String hiveVersion;
    private final String inputFormat;
    private final String outputFormat;
    private final String serializationLib;

    private final PartitionPolicy partitionPolicy;

    private final String partitionField;

    private final String timePattern;

    public HiveRowDataPartitionComputer(
            JobConf jobConf,
            HiveShim hiveShim,
            String hiveVersion,
            String defaultPartValue,
            String[] columnNames,
            DataType[] columnTypes,
            String[] partitionColumns,
            PartitionPolicy partitionPolicy,
            String partitionField,
            String timePattern,
            String inputFormat,
            String outputFormat,
            String serializationLib) {
        super(defaultPartValue, columnNames, columnTypes, partitionColumns);
        this.hiveShim = hiveShim;
        this.hiveVersion = hiveVersion;
        this.partitionConverters =
                Arrays.stream(partitionTypes)
                        .map(TypeConversions::fromLogicalToDataType)
                        .map(DataFormatConverters::getConverterForDataType)
                        .toArray(DataFormatConverters.DataFormatConverter[]::new);
        this.hiveObjectConversions = new HiveObjectConversion[partitionIndexes.length];
        for (int i = 0; i < hiveObjectConversions.length; i++) {
            DataType partColType = columnTypes[partitionIndexes[i]];
            ObjectInspector objectInspector = HiveInspectors.getObjectInspector(partColType);
            hiveObjectConversions[i] =
                    HiveInspectors.getConversion(
                            objectInspector, partColType.getLogicalType(), hiveShim);
        }
        this.sinkMultipleEnable = Boolean.parseBoolean(jobConf.get(SINK_MULTIPLE_ENABLE.key(), "false"));
        this.sinkMultipleFormat = jobConf.get(SINK_MULTIPLE_FORMAT.key());
        this.databasePattern = jobConf.get(SINK_MULTIPLE_DATABASE_PATTERN.key());
        this.tablePattern = jobConf.get(SINK_MULTIPLE_TABLE_PATTERN.key());
        this.partitionPolicy = partitionPolicy;
        this.partitionField = partitionField;
        this.timePattern = timePattern;
        this.inputFormat = inputFormat;
        this.outputFormat = outputFormat;
        this.serializationLib = serializationLib;
    }

    @Override
    public LinkedHashMap<String, String> generatePartValues(RowData in) {
        LinkedHashMap<String, String> partSpec = new LinkedHashMap<>();

        if (sinkMultipleEnable) {
            GenericRowData rowData = (GenericRowData) in;
            JsonNode rootNode;
            try {
                if (jsonFormat == null) {
                    jsonFormat = (JsonDynamicSchemaFormat) DynamicSchemaFormatFactory.getFormat(sinkMultipleFormat);
                }
                rootNode = jsonFormat.deserialize((byte[]) rowData.getField(0));

                String databaseName = jsonFormat.parse(rootNode, databasePattern);
                String tableName = jsonFormat.parse(rootNode, tablePattern);
                List<Map<String, Object>> physicalDataList = HiveTableUtil.jsonNode2Map(
                        jsonFormat.getPhysicalData(rootNode));

                List<String> pkListStr = jsonFormat.extractPrimaryKeyNames(rootNode);
                RowType schema = jsonFormat.extractSchema(rootNode, pkListStr);

                Map<String, Object> rawData = physicalDataList.get(0);
                ObjectIdentifier identifier = HiveTableUtil.createObjectIdentifier(databaseName, tableName);

                HashMap<ObjectIdentifier, Long> ignoreWritingTableMap = CacheHolder.getIgnoreWritingTableMap();
                // ignore writing data into this table
                if (ignoreWritingTableMap.containsKey(identifier)) {
                    return partSpec;
                }

                HiveWriterFactory hiveWriterFactory = HiveTableUtil.getWriterFactory(hiveShim, hiveVersion, identifier);
                if (hiveWriterFactory == null) {
                    HiveTableUtil.createTable(databaseName, tableName, schema, partitionPolicy, hiveVersion,
                            inputFormat, outputFormat, serializationLib);
                    hiveWriterFactory = HiveTableUtil.getWriterFactory(hiveShim, hiveVersion, identifier);
                }

                String[] partitionColumns = hiveWriterFactory.getPartitionColumns();
                if (partitionColumns.length == 0) {
                    // non partition table
                    return partSpec;
                }

                String[] columnNames = hiveWriterFactory.getAllColumns();
                DataType[] allTypes = hiveWriterFactory.getAllTypes();
                List<String> columnList = Arrays.asList(columnNames);
                int[] partitionIndexes = Arrays.stream(partitionColumns).mapToInt(columnList::indexOf).toArray();

                boolean replaceLineBreak = hiveWriterFactory.getStorageDescriptor().getInputFormat()
                        .contains("TextInputFormat");
                Pair<GenericRowData, Integer> pair = HiveTableUtil.getRowData(rawData, columnNames, allTypes,
                        replaceLineBreak);
                GenericRowData genericRowData = pair.getLeft();

                Object field;
                for (int i = 0; i < partitionIndexes.length; i++) {
                    int fieldIndex = partitionIndexes[i];
                    field = genericRowData.getField(fieldIndex);
                    DataType type = allTypes[fieldIndex];
                    ObjectInspector objectInspector = HiveInspectors.getObjectInspector(type);
                    HiveObjectConversion hiveConversion = HiveInspectors.getConversion(objectInspector,
                            type.getLogicalType(), hiveShim);
                    String partitionValue = field != null ? String.valueOf(hiveConversion.toHiveObject(field)) : null;
                    if (partitionValue == null) {
                        field = HiveTableUtil.getDefaultPartitionValue(rawData, schema, partitionPolicy, partitionField,
                                timePattern);
                        partitionValue = field != null ? String.valueOf(hiveConversion.toHiveObject(field)) : null;
                    }
                    if (StringUtils.isEmpty(partitionValue)) {
                        partitionValue = defaultPartValue;
                    }
                    partSpec.put(partitionColumns[i], partitionValue);
                }
            } catch (Exception e) {
                // do not throw exception, just log it. so HadoopPathBasedPartFileWriter will archive dirty data or not
                LOG.error("Generate partition values error", e);
            }
        } else {
            for (int i = 0; i < partitionIndexes.length; i++) {
                Object field = partitionConverters[i].toExternal(in, partitionIndexes[i]);
                String partitionValue = field != null ? hiveObjectConversions[i].toHiveObject(field).toString() : null;
                if (StringUtils.isEmpty(partitionValue)) {
                    partitionValue = defaultPartValue;
                }
                partSpec.put(partitionColumns[i], partitionValue);
            }
        }
        return partSpec;
    }
}
