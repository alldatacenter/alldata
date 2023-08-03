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

package org.apache.inlong.sort.base;

import org.apache.inlong.sort.base.sink.PartitionPolicy;
import org.apache.inlong.sort.base.sink.SchemaUpdateExceptionPolicy;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

import java.util.Map;

import static org.apache.inlong.common.constant.Constants.METRICS_AUDIT_PROXY_HOSTS_KEY;

/**
 * connector base option constant
 */
public final class Constants {

    /**
     * constants for metrics
     */
    public static final String DIRTY_BYTES_OUT = "dirtyBytesOut";

    public static final String DIRTY_RECORDS_OUT = "dirtyRecordsOut";

    public static final String NUM_BYTES_OUT = "numBytesOut";

    public static final String NUM_RECORDS_OUT = "numRecordsOut";

    public static final String NUM_BYTES_OUT_FOR_METER = "numBytesOutForMeter";

    public static final String NUM_RECORDS_OUT_FOR_METER = "numRecordsOutForMeter";

    public static final String NUM_BYTES_OUT_PER_SECOND = "numBytesOutPerSecond";

    public static final String NUM_RECORDS_OUT_PER_SECOND = "numRecordsOutPerSecond";

    public static final String NUM_RECORDS_IN = "numRecordsIn";

    public static final String NUM_BYTES_IN = "numBytesIn";

    public static final String NUM_RECORDS_IN_FOR_METER = "numRecordsInForMeter";

    public static final String NUM_BYTES_IN_FOR_METER = "numBytesInForMeter";

    public static final String NUM_BYTES_IN_PER_SECOND = "numBytesInPerSecond";

    public static final String NUM_RECORDS_IN_PER_SECOND = "numRecordsInPerSecond";

    public static final String CURRENT_FETCH_EVENT_TIME_LAG = "currentFetchEventTimeLag";

    public static final String CURRENT_EMIT_EVENT_TIME_LAG = "currentEmitEventTimeLag";

    /**
     * Timestamp when the read phase changed
     */
    public static final String READ_PHASE_TIMESTAMP = "readPhaseTimestamp";
    /**
     * Time span in seconds
     */
    public static final Integer TIME_SPAN_IN_SECONDS = 60;
    /**
     * Stream id used in inlong metric
     */
    public static final String STREAM_ID = "streamId";
    /**
     * Group id used in inlong metric
     */
    public static final String GROUP_ID = "groupId";
    /**
     * Node id used in inlong metric
     */
    public static final String NODE_ID = "nodeId";
    // sort received successfully
    public static final String AUDIT_SORT_INPUT = "7";

    // sort send successfully
    public static final Integer AUDIT_SORT_OUTPUT = 8;
    /**
     * Database Name used in inlong metric
     */
    public static final String DATABASE_NAME = "database";
    /**
     * Table Name used in inlong metric
     */
    public static final String TABLE_NAME = "table";
    /**
     * Collection Name used in inlong metric
     */
    public static final String COLLECTION_NAME = "collection";
    /**
     * Read Phase used in inlong metric
     */
    public static final String READ_PHASE = "readPhase";
    /**
     * Schema Name used in inlong metric
     */
    public static final String SCHEMA_NAME = "schema";
    /**
     * Topic Name used in inlong metric
     */
    public static final String TOPIC_NAME = "topic";
    /**
     * It is used for 'inlong.metric.labels' or 'sink.dirty.labels'
     */
    public static final String DELIMITER = "&";
    /**
     * It is used for metric data to build schema identify
     */
    public static final String SEMICOLON = ".";

    /**
     * The caret symbol (^) at the start of a regular expression to indicate
     * that a match must occur at the beginning of the searched text.
     */
    public static final String CARET = "^";

    /**
     * The dollar symbol ($) at the end of a regular expression to indicate
     * that a match must occur at the ending of the searched text.
     */
    public static final String DOLLAR = "$";
    /**
     * It is used for metric data to spilt schema identify
     */
    public static final String SPILT_SEMICOLON = "\\.";
    /**
     * The delimiter of key and value, it is used for 'inlong.metric.labels' or 'sink.dirty.labels'
     */
    public static final String KEY_VALUE_DELIMITER = "=";

    public static final String INLONG_METRIC_STATE_NAME = "inlong-metric-states";

    /**
     * It is used for jdbc url filter for avoiding url attack
     * see also in https://su18.org/post/jdbc-connection-url-attack/
     */
    public static final String AUTO_DESERIALIZE = "autoDeserialize";

    public static final String AUTO_DESERIALIZE_TRUE = "autoDeserialize=true";

    public static final String AUTO_DESERIALIZE_FALSE = "autoDeserialize=false";

    public static final String DDL_FIELD_NAME = "ddl";

    public static final String DDL_OP_ALTER = "ALTER";

    public static final String DDL_OP_DROP = "DROP";

    public static final String GHOST_TAG = "/* gh-ost */";

    public static final ConfigOption<String> INLONG_METRIC =
            ConfigOptions.key("inlong.metric.labels")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("INLONG metric labels, format is 'key1=value1&key2=value2',"
                            + "default is 'groupId=xxx&streamId=xxx&nodeId=xxx'");

    public static final ConfigOption<String> INLONG_AUDIT =
            ConfigOptions.key(METRICS_AUDIT_PROXY_HOSTS_KEY)
                    .stringType()
                    .noDefaultValue()
                    .withDescription("Audit proxy host address for reporting audit metrics. \n"
                            + "e.g. 127.0.0.1:10081,0.0.0.1:10081");

    public static final ConfigOption<String> AUDIT_KEYS =
            ConfigOptions.key("metrics.audit.key")
                    .stringType()
                    .defaultValue("")
                    .withDescription("Audit keys for metrics collecting");

    public static final ConfigOption<Boolean> IGNORE_ALL_CHANGELOG =
            ConfigOptions.key("sink.ignore.changelog")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Regard upsert delete as insert kind.");

    public static final ConfigOption<String> SINK_MULTIPLE_FORMAT =
            ConfigOptions.key("sink.multiple.format")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "The format of multiple sink, it represents the real format of the raw binary data");
    public static final ConfigOption<String> PATTERN_PARTITION_MAP =
            ConfigOptions.key("pattern.partition.map")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Pattern rules and partition map string, " +
                                    "eg: databasePattern1&tablePattern1:partition1,"
                                    + "databasePattern2&tablePattern2:partition2,"
                                    + "DEFAULT_PARTITION:partition3");
    public static final ConfigOption<Map<String, String>> DATASOURCE_PARTITION_MAP =
            ConfigOptions.key("datasource.partition.map")
                    .mapType()
                    .noDefaultValue()
                    .withDescription(
                            "Datasource and partition maps, eg: <datasource1,partition1>");
    public static final ConfigOption<String> SINK_MULTIPLE_DATABASE_PATTERN =
            ConfigOptions.key("sink.multiple.database-pattern")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The option 'sink.multiple.database-pattern' "
                            + "is used extract database name from the raw binary data, "
                            + "this is only used in the multiple sink writing scenario.");

    public static final ConfigOption<Boolean> SOURCE_MULTIPLE_ENABLE =
            ConfigOptions.key("source.multiple.enable")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether enable migrate multiple databases");

    public static final ConfigOption<String> SINK_MULTIPLE_TABLE_PATTERN =
            ConfigOptions.key("sink.multiple.table-pattern")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The option 'sink.multiple.table-pattern' "
                            + "is used extract table name from the raw binary data, "
                            + "this is only used in the multiple sink writing scenario.");

    public static final ConfigOption<Boolean> SINK_MULTIPLE_ENABLE =
            ConfigOptions.key("sink.multiple.enable")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("The option 'sink.multiple.enable' "
                            + "is used to determine whether to support multiple sink writing, default is 'false'.");

    public static final ConfigOption<SchemaUpdateExceptionPolicy> SINK_MULTIPLE_SCHEMA_UPDATE_POLICY =
            ConfigOptions.key("sink.multiple.schema-update.policy")
                    .enumType(SchemaUpdateExceptionPolicy.class)
                    .defaultValue(SchemaUpdateExceptionPolicy.TRY_IT_BEST)
                    .withDescription("The action to deal with schema update in multiple sink.");

    public static final ConfigOption<Boolean> SINK_MULTIPLE_IGNORE_SINGLE_TABLE_ERRORS =
            ConfigOptions.key("sink.multiple.ignore-single-table-errors")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether ignore the single table erros when multiple sink writing scenario.");

    public static final ConfigOption<Boolean> SINK_MULTIPLE_PK_AUTO_GENERATED =
            ConfigOptions.key("sink.multiple.pk-auto-generated")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether generated pk fields as whole data when source table does not have a "
                            + "primary key.");

    public static final ConfigOption<Boolean> SINK_MULTIPLE_TYPE_MAP_COMPATIBLE_WITH_SPARK =
            ConfigOptions.key("sink.multiple.typemap-compatible-with-spark")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Because spark do not support iceberg data type: `timestamp without time zone` and"
                            + "`time`, so type conversions must be mapped to types supported by spark.");

    public static final ConfigOption<PartitionPolicy> SINK_PARTITION_POLICY =
            ConfigOptions.key("sink.partition.policy")
                    .enumType(PartitionPolicy.class)
                    .defaultValue(PartitionPolicy.PROC_TIME)
                    .withDescription("The policy of partitioning table.");

    public static final ConfigOption<String> SOURCE_PARTITION_FIELD_NAME =
            ConfigOptions.key("source.partition.field.name")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "The field name in source generic raw data, partition table by the field value dynamically."
                                    + "Support regex expressions to match many fields in source generic raw data.");

    // ========================================= dirty configuration =========================================
    public static final String DIRTY_PREFIX = "dirty.";

    public static final ConfigOption<Boolean> DIRTY_IGNORE =
            ConfigOptions.key("dirty.ignore")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether ignore the dirty data, default value is 'false'");
    public static final ConfigOption<String> DIRTY_IDENTIFIER =
            ConfigOptions.key("dirty.identifier")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "The identifier of dirty data, it will be used for filename generation of file dirty sink, "
                                    + "topic generation of mq dirty sink, tablename generation of database, etc."
                                    + "and it supports variable replace like '${variable}'."
                                    + "There are several system variables[SYSTEM_TIME|DIRTY_TYPE|DIRTY_MESSAGE] "
                                    + "are currently supported, "
                                    + "and the support of other variables is determined by the connector.");
    public static final ConfigOption<Boolean> DIRTY_SIDE_OUTPUT_ENABLE =
            ConfigOptions.key("dirty.side-output.enable")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether supports dirty data side-output, default value is 'false'");
    public static final ConfigOption<String> DIRTY_SIDE_OUTPUT_CONNECTOR =
            ConfigOptions.key("dirty.side-output.connector")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The connector of dirty side-output");
    public static final ConfigOption<String> DIRTY_SIDE_OUTPUT_FORMAT =
            ConfigOptions.key("dirty.side-output.format")
                    .stringType()
                    .defaultValue("csv")
                    .withDescription(
                            "The format of dirty side-output, only support [csv|json] for now and default value is 'csv'");
    public static final ConfigOption<Boolean> DIRTY_SIDE_OUTPUT_IGNORE_ERRORS =
            ConfigOptions.key("dirty.side-output.ignore-errors")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether ignore the dirty side-output erros, default value is 'true'.");
    public static final ConfigOption<Boolean> DIRTY_SIDE_OUTPUT_LOG_ENABLE =
            ConfigOptions.key("dirty.side-output.log.enable")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Whether enable log print, default value is 'true'.");
    public static final ConfigOption<String> DIRTY_SIDE_OUTPUT_LABELS =
            ConfigOptions.key("dirty.side-output.labels")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "The labels of dirty side-output, format is 'key1=value1&key2=value2', "
                                    + "it supports variable replace like '${variable}',"
                                    + "There are two system variables[SYSTEM_TIME|DIRTY_TYPE|DIRTY_MESSAGE] "
                                    + "are currently supported,"
                                    + " and the support of other variables is determined by the connector.");
    public static final ConfigOption<String> DIRTY_SIDE_OUTPUT_LOG_TAG =
            ConfigOptions.key("dirty.side-output.log-tag")
                    .stringType()
                    .defaultValue("DirtyData")
                    .withDescription(
                            "The log tag of dirty side-output, it supports variable replace like '${variable}'."
                                    + "There are two system variables[SYSTEM_TIME|DIRTY_TYPE|DIRTY_MESSAGE] are currently supported,"
                                    + " and the support of other variables is determined by the connector.");
    public static final ConfigOption<String> DIRTY_SIDE_OUTPUT_FIELD_DELIMITER =
            ConfigOptions.key("dirty.side-output.field-delimiter")
                    .stringType()
                    .defaultValue(",")
                    .withDescription("The field-delimiter of dirty side-output");
    public static final ConfigOption<String> DIRTY_SIDE_OUTPUT_LINE_DELIMITER =
            ConfigOptions.key("dirty.side-output.line-delimiter")
                    .stringType()
                    .defaultValue("\n")
                    .withDescription("The line-delimiter of dirty sink");
    public static final ConfigOption<Integer> DIRTY_SIDE_OUTPUT_BATCH_SIZE = ConfigOptions
            .key("dirty.side-output.batch.size")
            .intType()
            .defaultValue(100)
            .withDescription(
                    "The flush max size, over this number of records, will flush data. The default value is 100.");
    public static final ConfigOption<Integer> DIRTY_SIDE_OUTPUT_RETRIES = ConfigOptions
            .key("dirty.side-output.retries")
            .intType()
            .defaultValue(3)
            .withDescription("The retry times if writing records failed.");
    public static final ConfigOption<Long> DIRTY_SIDE_OUTPUT_BATCH_INTERVAL = ConfigOptions
            .key("dirty.side-output.batch.interval")
            .longType()
            .defaultValue(60000L)
            .withDescription(
                    "The flush interval mills, over this time, "
                            + "asynchronous threads will flush data. The default value is 60s.");
    public static final ConfigOption<Long> DIRTY_SIDE_OUTPUT_BATCH_BYTES = ConfigOptions
            .key("dirty.side-output.batch.bytes")
            .longType()
            .defaultValue(10240L)
            .withDescription(
                    "The flush max bytes, over this number in batch, will flush data. The default value is 10KB.");
    public static final ConfigOption<Boolean> GH_OST_DDL_CHANGE = ConfigOptions
            .key("gh-ost.ddl.change")
            .booleanType()
            .defaultValue(false)
            .withDescription(
                    "Whether parse ddl changes of gh-ost, default value is 'false'.");
    public static final ConfigOption<String> GH_OST_TABLE_REGEX = ConfigOptions
            .key("gh-ost.table.regex")
            .stringType()
            .defaultValue("^_(.*)_(gho|ghc|del)$")
            .withDescription(
                    "Matcher the original table name from the ddl of gh-ost.");

    public static final ConfigOption<Boolean> SINK_SCHEMA_CHANGE_ENABLE =
            ConfigOptions.key("sink.schema-change.enable")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether supports schema-change, default value is 'false'");

    public static final ConfigOption<String> SINK_SCHEMA_CHANGE_POLICIES =
            ConfigOptions.key("sink.schema-change.policies")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The policies of schema-change, format is 'key1=value1&key2=value2', "
                            + "the key is the type of schema-change and the value is the support policy of schema-change");
}
