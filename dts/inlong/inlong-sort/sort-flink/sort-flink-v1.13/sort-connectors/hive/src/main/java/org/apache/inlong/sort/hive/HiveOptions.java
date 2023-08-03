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

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

import static org.apache.flink.configuration.ConfigOptions.key;

/**
 * This class holds configuration constants used by hive connector.
 */
public class HiveOptions {

    public static final ConfigOption<String> HIVE_DATABASE =
            ConfigOptions.key("hive-database")
                    .stringType()
                    .noDefaultValue()
                    .withDescription("The name of hive database to connect.");

    public static final ConfigOption<Boolean> TABLE_EXEC_HIVE_FALLBACK_MAPRED_READER =
            key("table.exec.hive.fallback-mapred-reader")
                    .defaultValue(false)
                    .withDescription(
                            "If it is false, using flink native vectorized reader to read orc files; "
                                    + "If it is true, using hadoop mapred record reader to read orc files.");

    public static final ConfigOption<Boolean> TABLE_EXEC_HIVE_INFER_SOURCE_PARALLELISM =
            key("table.exec.hive.infer-source-parallelism")
                    .defaultValue(true)
                    .withDescription(
                            "If is false, parallelism of source are set by config.\n"
                                    + "If is true, source parallelism is inferred according to splits number.\n");

    public static final ConfigOption<Integer> TABLE_EXEC_HIVE_INFER_SOURCE_PARALLELISM_MAX =
            key("table.exec.hive.infer-source-parallelism.max")
                    .defaultValue(1000)
                    .withDescription("Sets max infer parallelism for source operator.");

    public static final ConfigOption<Boolean> TABLE_EXEC_HIVE_FALLBACK_MAPRED_WRITER =
            key("table.exec.hive.fallback-mapred-writer")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription(
                            "If it is false, using flink native writer to write parquet and orc files; "
                                    + "If it is true, using hadoop mapred record writer to write "
                                    + "parquet and orc files.");

    public static final ConfigOption<Boolean> HIVE_IGNORE_ALL_CHANGELOG =
            ConfigOptions.key("sink.ignore.changelog")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Regard upsert delete as insert kind.");

    public static final ConfigOption<String> SINK_PARTITION_NAME =
            ConfigOptions.key("sink.partition.name")
                    .stringType()
                    .defaultValue("pt")
                    .withDescription("The default partition name for creating new hive table.");

    public static final ConfigOption<Integer> HIVE_SCHEMA_SCAN_INTERVAL =
            ConfigOptions.key("sink.schema.scan.interval")
                    .intType()
                    .defaultValue(10)
                    .withDescription("The interval milliseconds to scan if source table schema changed.");

    public static final ConfigOption<String> HIVE_STORAGE_INPUT_FORMAT =
            ConfigOptions.key("hive.storage.input.format")
                    .stringType()
                    .defaultValue("org.apache.hadoop.mapred.TextInputFormat")
                    .withDescription("The input format of storage descriptor");

    public static final ConfigOption<String> HIVE_STORAGE_OUTPUT_FORMAT =
            ConfigOptions.key("hive.storage.output.format")
                    .stringType()
                    .defaultValue("org.apache.hadoop.hive.ql.io.HiveIgnoreKeyTextOutputFormat")
                    .withDescription("The output format of storage descriptor");

    public static final ConfigOption<String> HIVE_STORAGE_SERIALIZATION_LIB =
            ConfigOptions.key("hive.storage.serialization.lib")
                    .stringType()
                    .defaultValue("org.apache.hadoop.hive.serde2.lazy.LazySimpleSerDe")
                    .withDescription("The serialization library of storage descriptor");

}
