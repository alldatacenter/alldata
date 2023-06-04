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

package org.apache.inlong.sort.kudu.common;

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

/**
 * The configuration options for kudu sink.
 */
public class KuduOptions {

    public static final ConfigOption<String> CONNECTOR_TABLE =
            ConfigOptions.key("table-name")
                    .stringType()
                    .noDefaultValue().withDescription("The name of kudu table.");

    public static final ConfigOption<String> CONNECTOR_MASTERS =
            ConfigOptions.key("masters")
                    .stringType()
                    .noDefaultValue().withDescription(" The masters of kudu server.");

    public static final ConfigOption<String> FLUSH_MODE =
            ConfigOptions.key("flush-mode")
                    .stringType()
                    .defaultValue("AUTO_FLUSH_SYNC")
                    .withDescription(
                            "The flush mode of Kudu client session, AUTO_FLUSH_SYNC/AUTO_FLUSH_BACKGROUND/MANUAL_FLUSH.");

    public static final ConfigOption<Integer> MAX_CACHE_SIZE =
            ConfigOptions.key("lookup.max-cache-size")
                    .intType()
                    .defaultValue(-1)
                    .withDescription("The maximum number of results cached in the " +
                            "lookup source.");

    public static final ConfigOption<Long> DEFAULT_ADMIN_OPERATION_TIMEOUT_IN_MS =
            ConfigOptions.key("default-admin-operation-timeout")
                    .longType()
                    .defaultValue(30000L)
                    .withDescription(
                            "Sets the default timeout used for administrative operations (e.g. createTable, deleteTable, etc). Optional. If not provided, defaults to 30s. A value of 0 disables the timeout.");

    public static final ConfigOption<Long> DEFAULT_OPERATION_TIMEOUT_IN_MS =
            ConfigOptions.key("default-operation-timeout")
                    .longType()
                    .defaultValue(30000L)
                    .withDescription(
                            "Sets the default timeout used for user operations (using sessions and scanners). Optional. If not provided, defaults to 30s. A value of 0 disables the timeout.");

    public static final ConfigOption<Long> DEFAULT_SOCKET_READ_TIMEOUT_IN_MS =
            ConfigOptions.key("default-socket-read-timeout")
                    .longType()
                    .defaultValue(10000L)
                    .withDescription("Default socket read timeout in ms, default is 10000");
    public static final ConfigOption<Boolean> DISABLED_STATISTICS =
            ConfigOptions.key("disabled-statistics")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription(
                            "Disable this client's collection of statistics. Statistics are enabled by default.");

    public static final ConfigOption<String> MAX_CACHE_TIME =
            ConfigOptions.key("lookup.max-cache-time")
                    .stringType()
                    .defaultValue("60s")
                    .withDescription("The maximum live time for cached results in " +
                            "the lookup source.");
    public static final ConfigOption<Boolean> SINK_START_NEW_CHAIN =
            ConfigOptions.key("sink.start-new-chain")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("The sink operator will start a new chain if true.");

    public static final ConfigOption<Integer> MAX_RETRIES =
            ConfigOptions.key("max-retries")
                    .intType()
                    .defaultValue(3)
                    .withDescription("The maximum number of retries when an " +
                            "exception is caught.");

    public static final ConfigOption<Integer> MAX_BUFFER_SIZE =
            ConfigOptions.key("sink.max-buffer-size")
                    .intType()
                    .defaultValue(100)
                    .withDescription("The maximum number of records buffered in the sink.");

    public static final ConfigOption<Integer> WRITE_THREAD_COUNT =
            ConfigOptions.key("sink.write-thread-count")
                    .intType()
                    .defaultValue(5)
                    .withDescription("The maximum number of thread in the sink.");

    public static final ConfigOption<String> MAX_BUFFER_TIME =
            ConfigOptions.key("sink.max-buffer-time")
                    .stringType()
                    .defaultValue("30s")
                    .withDescription("The maximum wait time for buffered records in the sink.");

    public static final ConfigOption<String> SINK_KEY_FIELD_NAMES =
            ConfigOptions.key("sink.key-field-names")
                    .stringType()
                    .defaultValue("")
                    .withDescription("The key fields for updating DB when using upsert sink function.");
    public static final ConfigOption<Boolean> ENABLE_KEY_FIELD_CHECK =
            ConfigOptions.key("sink.enable-key-field-check")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("If true, the check to compare key fields assumed by flink " +
                            "and key fields provided by user will be performed.");

    public static final ConfigOption<Boolean> SINK_WRITE_WITH_ASYNC_MODE =
            ConfigOptions.key("sink.write-with-async-mode")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Use async write mode for kudu producer if true.");

    public static final ConfigOption<Boolean> SINK_FORCE_WITH_UPSERT_MODE =
            ConfigOptions.key("sink.write-with-upsert-mode")
                    .booleanType()
                    .defaultValue(true)
                    .withDescription("Force write kudu with upsert mode if true.");

    public static final ConfigOption<Integer> CACHE_QUEUE_MAX_LENGTH =
            ConfigOptions.key("sink.cache-queue-max-length")
                    .intType()
                    .defaultValue(-1)
                    .withDescription("The maximum queue lengths.");

    public static final ConfigOption<Boolean> IGNORE_ALL_CHANGELOG =
            ConfigOptions.key("sink.ignore-all-changelog")
                    .booleanType()
                    .defaultValue(false)
                    .withDescription("Whether ignore delete/update_before/update_after message.");
}
