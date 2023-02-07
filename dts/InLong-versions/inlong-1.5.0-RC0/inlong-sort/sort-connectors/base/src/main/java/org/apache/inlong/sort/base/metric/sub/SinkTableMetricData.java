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

package org.apache.inlong.sort.base.metric.sub;

import com.google.common.collect.Maps;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.metrics.MetricGroup;
import org.apache.inlong.sort.base.Constants;
import org.apache.inlong.sort.base.metric.MetricOption;
import org.apache.inlong.sort.base.metric.MetricOption.RegisteredMetric;
import org.apache.inlong.sort.base.metric.MetricState;
import org.apache.inlong.sort.base.metric.SinkMetricData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import static org.apache.inlong.sort.base.Constants.DELIMITER;
import static org.apache.inlong.sort.base.Constants.DIRTY_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.DIRTY_RECORDS_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_BYTES_OUT;
import static org.apache.inlong.sort.base.Constants.NUM_RECORDS_OUT;

/**
 * A collection class for handling sub metrics of table schema type
 */
public class SinkTableMetricData extends SinkMetricData implements SinkSubMetricData {

    public static final Logger LOGGER = LoggerFactory.getLogger(SinkTableMetricData.class);

    /**
     * The sub sink metric data container of sink metric data
     */
    private final Map<String, SinkMetricData> subSinkMetricMap = Maps.newHashMap();

    public SinkTableMetricData(MetricOption option, MetricGroup metricGroup) {
        super(option, metricGroup);
    }

    /**
     * register sub sink metrics group from metric state
     *
     * @param metricState MetricState
     */
    public void registerSubMetricsGroup(MetricState metricState) {
        if (metricState == null) {
            return;
        }

        // register sub sink metric data
        if (metricState.getSubMetricStateMap() == null) {
            return;
        }
        Map<String, MetricState> subMetricStateMap = metricState.getSubMetricStateMap();
        for (Entry<String, MetricState> subMetricStateEntry : subMetricStateMap.entrySet()) {
            String[] schemaInfoArray = parseSchemaIdentify(subMetricStateEntry.getKey());
            final MetricState subMetricState = subMetricStateEntry.getValue();
            SinkMetricData subSinkMetricData = buildSubSinkMetricData(schemaInfoArray, subMetricState, this);
            subSinkMetricMap.put(subMetricStateEntry.getKey(), subSinkMetricData);
        }
        LOGGER.info("register subMetricsGroup from metricState,sub metric map size:{}", subSinkMetricMap.size());
    }

    /**
     * build sub sink metric data
     *
     * @param schemaInfoArray sink record schema info
     * @param sinkMetricData sink metric data
     * @return sub sink metric data
     */
    private SinkMetricData buildSubSinkMetricData(String[] schemaInfoArray, SinkMetricData sinkMetricData) {
        return buildSubSinkMetricData(schemaInfoArray, null, sinkMetricData);
    }

    /**
     * build sub sink metric data
     *
     * @param schemaInfoArray the schema info array of record
     * @param subMetricState sub metric state
     * @param sinkMetricData sink metric data
     * @return sub sink metric data
     */
    private SinkMetricData buildSubSinkMetricData(String[] schemaInfoArray, MetricState subMetricState,
            SinkMetricData sinkMetricData) {
        if (sinkMetricData == null || schemaInfoArray == null) {
            return null;
        }
        // build sub sink metric data
        Map<String, String> labels = sinkMetricData.getLabels();
        String metricGroupLabels = labels.entrySet().stream().map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining(DELIMITER));
        StringBuilder labelBuilder = new StringBuilder(metricGroupLabels);
        if (schemaInfoArray.length == 2) {
            labelBuilder.append(DELIMITER).append(Constants.DATABASE_NAME).append("=").append(schemaInfoArray[0])
                    .append(DELIMITER).append(Constants.TABLE_NAME).append("=").append(schemaInfoArray[1]);
        } else if (schemaInfoArray.length == 3) {
            labelBuilder.append(DELIMITER).append(Constants.DATABASE_NAME).append("=").append(schemaInfoArray[0])
                    .append(DELIMITER).append(Constants.SCHEMA_NAME).append("=").append(schemaInfoArray[1])
                    .append(DELIMITER).append(Constants.TABLE_NAME).append("=").append(schemaInfoArray[2]);
        }

        MetricOption metricOption = MetricOption.builder()
                .withInitRecords(subMetricState != null ? subMetricState.getMetricValue(NUM_RECORDS_OUT) : 0L)
                .withInitBytes(subMetricState != null ? subMetricState.getMetricValue(NUM_BYTES_OUT) : 0L)
                .withInitDirtyRecords(subMetricState != null ? subMetricState.getMetricValue(DIRTY_RECORDS_OUT) : 0L)
                .withInitDirtyBytes(subMetricState != null ? subMetricState.getMetricValue(DIRTY_BYTES_OUT) : 0L)
                .withInlongLabels(labelBuilder.toString()).withRegisterMetric(RegisteredMetric.ALL).build();
        return new SinkTableMetricData(metricOption, sinkMetricData.getMetricGroup());
    }

    /**
     * build record schema identify,in the form of database.schema.table or database.table
     *
     * @param database the database name of record
     * @param schema the schema name of record
     * @param table the table name of record
     * @return the record schema identify
     */
    public String buildSchemaIdentify(String database, String schema, String table) {
        if (schema == null) {
            return database + Constants.SEMICOLON + table;
        }
        return database + Constants.SEMICOLON + schema + Constants.SEMICOLON + table;
    }

    /**
     * parse record schema identify
     *
     * @param schemaIdentify the schema identify of record
     * @return the record schema identify array,String[]{database,table}
     */
    public String[] parseSchemaIdentify(String schemaIdentify) {
        return schemaIdentify.split(Constants.SPILT_SEMICOLON);
    }

    /**
     * output metrics with estimate
     *
     * @param database the database name of record
     * @param schema the schema name of record
     * @param table the table name of record
     * @param data the data of record
     */
    public void outputMetricsWithEstimate(String database, String schema, String table, Object data) {
        // sink metric and sub sink metric output metrics
        long rowCountSize = 1L;
        long rowDataSize = 0L;
        if (data != null) {
            rowDataSize = data.toString().getBytes(StandardCharsets.UTF_8).length;
        }
        outputMetrics(database, schema, table, rowCountSize, rowDataSize);
    }

    /**
     * output metrics
     *
     * @param database the database name of record
     * @param schema the schema name of record
     * @param table the table name of record
     * @param rowCount the row count of records
     * @param rowSize the row size of records
     */
    public void outputMetrics(String database, String schema, String table, long rowCount, long rowSize) {
        if (StringUtils.isBlank(database) || StringUtils.isBlank(table)) {
            invoke(rowCount, rowSize);
            return;
        }
        String identify = buildSchemaIdentify(database, schema, table);
        SinkMetricData subSinkMetricData;
        if (subSinkMetricMap.containsKey(identify)) {
            subSinkMetricData = subSinkMetricMap.get(identify);
        } else {
            subSinkMetricData = buildSubSinkMetricData(new String[]{database, schema, table}, this);
            subSinkMetricMap.put(identify, subSinkMetricData);
        }
        // sink metric and sub sink metric output metrics
        this.invoke(rowCount, rowSize);
        subSinkMetricData.invoke(rowCount, rowSize);
    }

    /**
     * output dirty metrics with estimate
     *
     * @param database the database name of record
     * @param schema the schema name of record
     * @param table the table name of record
     * @param rowCount the row count of records
     * @param rowSize the row size of records
     */
    public void outputDirtyMetricsWithEstimate(String database, String table, long rowCount,
            long rowSize) {
        if (StringUtils.isBlank(database) || StringUtils.isBlank(table)) {
            invokeDirty(rowCount, rowSize);
            return;
        }
        String identify = buildSchemaIdentify(database, null, table);
        SinkMetricData subSinkMetricData;
        if (subSinkMetricMap.containsKey(identify)) {
            subSinkMetricData = subSinkMetricMap.get(identify);
        } else {
            subSinkMetricData = buildSubSinkMetricData(new String[]{database, table}, this);
            subSinkMetricMap.put(identify, subSinkMetricData);
        }
        // sink metric and sub sink metric output metrics
        this.invokeDirty(rowCount, rowSize);
        subSinkMetricData.invokeDirty(rowCount, rowSize);
    }

    public void outputMetricsWithEstimate(Object data) {
        long size = data.toString().getBytes(StandardCharsets.UTF_8).length;
        invoke(1, size);
    }

    /**
     * output dirty metrics
     *
     * @param database the database name of record
     * @param schema the schema name of record
     * @param table the table name of record
     * @param rowCount the row count of records
     * @param rowSize the row size of records
     */
    public void outputDirtyMetrics(String database, String schema, String table, long rowCount, long rowSize) {
        if (StringUtils.isBlank(database) || StringUtils.isBlank(table)) {
            invokeDirty(rowCount, rowSize);
            return;
        }
        String identify = buildSchemaIdentify(database, schema, table);
        SinkMetricData subSinkMetricData;
        if (subSinkMetricMap.containsKey(identify)) {
            subSinkMetricData = subSinkMetricMap.get(identify);
        } else {
            subSinkMetricData = buildSubSinkMetricData(new String[]{database, schema, table}, this);
            subSinkMetricMap.put(identify, subSinkMetricData);
        }
        // sink metric and sub sink metric output metrics
        this.invokeDirty(rowCount, rowSize);
        subSinkMetricData.invokeDirty(rowCount, rowSize);
    }

    /**
     * output dirty metrics with estimate
     *
     * @param database the database name of record
     * @param schema the schema name of record
     * @param table the table name of record
     * @param data the dirty data
     */
    public void outputDirtyMetricsWithEstimate(String database, String schema, String table, Object data) {
        long size = data == null ? 0L : data.toString().getBytes(StandardCharsets.UTF_8).length;
        outputDirtyMetrics(database, schema, table, 1, size);
    }

    public void outputDirtyMetricsWithEstimate(Object data) {
        long size = data.toString().getBytes(StandardCharsets.UTF_8).length;
        invokeDirty(1, size);
    }

    @Override
    public Map<String, SinkMetricData> getSubSinkMetricMap() {
        return this.subSinkMetricMap;
    }

    @Override
    public String toString() {
        return "SinkTableMetricData{"
                + super.toString() + ","
                + "subSinkMetricMap=" + subSinkMetricMap + '}';
    }
}