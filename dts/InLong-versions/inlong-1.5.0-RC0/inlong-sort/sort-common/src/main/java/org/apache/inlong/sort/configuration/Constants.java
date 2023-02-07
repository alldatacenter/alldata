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

package org.apache.inlong.sort.configuration;

import java.time.Duration;
import static org.apache.inlong.sort.configuration.ConfigOptions.key;

/**
 * Constants used in sort
 */
public class Constants {

    public static final long UNKNOWN_DATAFLOW_ID = -1L;

    public static final String SOURCE_TYPE_TUBE = "tubemq";

    public static final String SOURCE_TYPE_PULSAR = "pulsar";

    public static final String SOURCE_TYPE_TDMQ_PULSAR = "tdmq_pulsar";

    public static final String SINK_TYPE_CLICKHOUSE = "clickhouse";

    public static final String SINK_TYPE_DORIS = "doris";

    public static final String SINK_TYPE_HIVE = "hive";

    public static final String SINK_TYPE_ICEBERG = "iceberg";

    public static final String SINK_TYPE_KAFKA = "kafka";

    public static final String SINK_TYPE_HBASE = "hbase";

    public static final String SINK_TYPE_ES = "elasticsearch";

    public static final String METRIC_DATA_OUTPUT_TAG_ID = "metric_data_side_output";

    public static final int METRIC_AUDIT_ID_FOR_INPUT = 7;

    public static final int METRIC_AUDIT_ID_FOR_OUTPUT = 8;

    public static final String INLONG_GROUP_ID = "inlong.group.id";

    public static final String HIVE_SINK_PREFIX = "hive.sink.";

    public static final String HIVE_SINK_ORC_PREFIX = HIVE_SINK_PREFIX + "orc.";

    public static final String GROUP_ID = "groupId";

    public static final String STREAM_ID = "streamId";

    public static final String NODE_ID = "nodeId";

    // ------------------------------------------------------------------------
    // Operator uid
    // ------------------------------------------------------------------------
    public static final String SOURCE_UID = "source_uid";

    public static final String DESERIALIZATION_SCHEMA_UID = "deserialization_schema_uid";

    public static final String TRANSFORMATION_UID = "transformation_uid";

    public static final String SINK_UID = "sink_uid";

    /**
     * It uses dt as the built-in data time field name. It's a work-around solution and should be replaced by later.
     */
    public static final String DATA_TIME_FIELD = "dt";

    /**
     * The prefix of source or sink configuration.
     */
    public static final String PULSAR_SOURCE_PREFIX = "pulsar.source.";

    // ------------------------------------------------------------------------
    // Common configs
    // ------------------------------------------------------------------------
    /**
     * The pipeline name is the key of configuration
     * that represents the configuration of {@link this#JOB_NAME} in Flink Table API
     */
    public static final String PIPELINE_NAME = "pipeline.name";

    /**
     * The ID of the cluster, used to separate multiple clusters.
     */
    public static final ConfigOption<String> CLUSTER_ID = key("cluster-id").noDefaultValue()
            .withDescription("The ID of the cluster, used to separate multiple clusters.");

    /**
     * The job name of this job, default is 'InLong-Sort-Job'
     */
    public static final ConfigOption<String> JOB_NAME = key("job.name").defaultValue("InLong-Sort-Job")
            .withDescription("The job name of this job");

    /**
     * The ZooKeeper quorum to use.
     */
    public static final ConfigOption<String> ZOOKEEPER_QUORUM = key("zookeeper.quorum").noDefaultValue()
            .withDescription("The ZooKeeper quorum to use");

    /**
     * The root path under which it stores its entries in ZooKeeper.
     */
    public static final ConfigOption<String> ZOOKEEPER_ROOT = key("zookeeper.path.root").defaultValue("/inlong-sort")
            .withDescription("The root path in ZooKeeper.");

    public static final ConfigOption<Integer> ETL_RECORD_SERIALIZATION_BUFFER_SIZE =
            key("etl.record.serialization.buffer.size").defaultValue(1024);

    public static final ConfigOption<String> SOURCE_TYPE = key("source.type").noDefaultValue()
            .withDescription("The type of source, currently only 'tubemq' is supported");

    public static final ConfigOption<String> SINK_TYPE = key("sink.type").noDefaultValue()
            .withDescription("The type of sink, currently only 'clickhouse' and 'iceberg' are supported");

    // ------------------------------------------------------------------------
    // Operator parallelism configs
    // ------------------------------------------------------------------------
    public static final ConfigOption<Integer> SOURCE_PARALLELISM = key("source.parallelism").defaultValue(1);

    public static final ConfigOption<Integer> DESERIALIZATION_PARALLELISM =
            key("deserialization.parallelism").defaultValue(1);

    public static final ConfigOption<Integer> TRANSFORMATION_PARALLELISM =
            key("transformation.parallelism").defaultValue(1);

    public static final ConfigOption<Integer> SINK_PARALLELISM = key("sink.parallelism").defaultValue(1);

    public static final ConfigOption<Integer> COMMITTER_PARALLELISM = key("committer.parallelism").defaultValue(1);

    // ------------------------------------------------------------------------
    // TubeMQ source configs
    // ------------------------------------------------------------------------
    public static final ConfigOption<String> TUBE_MASTER_ADDRESS = key("tubemq.master.address").noDefaultValue()
            .withDescription("The address of tubeMQ master.");

    public static final ConfigOption<String> TUBE_SESSION_KEY = key("tubemq.session.key").defaultValue("inlong-sort")
            .withDescription("The session key of tubeMQ consumer.");

    public static final ConfigOption<Boolean> TUBE_BOOTSTRAP_FROM_MAX = key("tubemq.bootstrap.from.max")
            .defaultValue(false)
            .withDescription("Consume tubeMQ from max offset.");

    public static final ConfigOption<String> TUBE_MESSAGE_NOT_FOUND_WAIT_PERIOD =
            key("tubemq.message.not.found.wait.period").defaultValue("350ms")
                    .withDescription("The time of waiting period if "
                            + "tubeMQ broker return message not found.");

    public static final ConfigOption<Long> TUBE_SUBSCRIBE_RETRY_TIMEOUT = key("tubemq.subscribe.retry.timeout")
            .defaultValue(300000L)
            .withDescription("The time of subscribing tubeMQ timeout, in millisecond");

    public static final ConfigOption<Integer> SOURCE_EVENT_QUEUE_CAPACITY =
            key("source.event.queue.capacity").defaultValue(1024);

    // ------------------------------------------------------------------------
    // ZooKeeper Client Settings
    // ------------------------------------------------------------------------

    public static final ConfigOption<Integer> ZOOKEEPER_SESSION_TIMEOUT = key("zookeeper.client.session-timeout")
            .defaultValue(60000)
            .withDescription("Defines the session timeout for the ZooKeeper session in ms.");

    public static final ConfigOption<Integer> ZOOKEEPER_CONNECTION_TIMEOUT = key("zookeeper.client.connection-timeout")
            .defaultValue(15000)
            .withDescription("Defines the connection timeout for ZooKeeper in ms.");

    public static final ConfigOption<Integer> ZOOKEEPER_RETRY_WAIT = key("zookeeper.client.retry-wait")
            .defaultValue(5000)
            .withDescription("Defines the pause between consecutive retries in ms.");

    public static final ConfigOption<Integer> ZOOKEEPER_MAX_RETRY_ATTEMPTS = key("zookeeper.client.max-retry-attempts")
            .defaultValue(3)
            .withDescription("Defines the number of connection retries before the client gives up.");

    public static final ConfigOption<String> ZOOKEEPER_CLIENT_ACL = key("zookeeper.client.acl")
            .defaultValue("open")
            .withDescription("Defines the ACL (open|creator) to be configured on ZK node. The "
                    + "configuration value can be"
                    + " set to “creator” if the ZooKeeper server configuration "
                    + "has the “authProvider” property mapped to use"
                    + " SASLAuthenticationProvider and the cluster is configured "
                    + "to run in secure mode (Kerberos).");

    public static final ConfigOption<Boolean> ZOOKEEPER_SASL_DISABLE =
            key("zookeeper.sasl.disable").defaultValue(false);

    // ------------------------------------------------------------------------
    // Sink field nullable related
    // ------------------------------------------------------------------------
    public static final ConfigOption<Boolean> SINK_FIELD_TYPE_STRING_NULLABLE = key("sink.field.type.string.nullable")
            .defaultValue(false)
            .withDescription("The default value of string is empty string.");

    public static final ConfigOption<Boolean> SINK_FIELD_TYPE_INT_NULLABLE =
            key("sink.field.type.int.nullable").defaultValue(true);

    public static final ConfigOption<Boolean> SINK_FIELD_TYPE_SHORT_NULLABLE =
            key("sink.field.type.short.nullable").defaultValue(true);

    public static final ConfigOption<Boolean> SINK_FIELD_TYPE_LONG_NULLABLE =
            key("sink.field.type.long.nullable").defaultValue(true);

    // ------------------------------------------------------------------------
    // Kafka sink related configs
    // ------------------------------------------------------------------------
    public static final ConfigOption<Integer> SINK_KAFKA_PRODUCER_POOL_SIZE =
            key("sink.kafka.producer.pool.size").defaultValue(5);

    // ------------------------------------------------------------------------
    // Hive sink related configs
    // ------------------------------------------------------------------------

    public static final ConfigOption<Integer> SINK_HIVE_COMMITTED_PARTITIONS_CACHE_SIZE =
            key("sink.hive.committed.partitions.cache.size").defaultValue(1024);

    public static final ConfigOption<Long> SINK_HIVE_ROLLING_POLICY_FILE_SIZE =
            key("sink.hive.rolling-policy.file-size").defaultValue(128L << 20)
                    .withDescription("The maximum part file size before rolling.");

    public static final ConfigOption<Long> SINK_HIVE_ROLLING_POLICY_ROLLOVER_INTERVAL =
            key("sink.hive.rolling-policy.rollover-interval")
                    .defaultValue(Duration.ofMinutes(30).toMillis())
                    .withDescription("The maximum time duration a part file can stay open before rolling"
                            + " (by default long enough to avoid too many small files). The frequency at which"
                            + " this is checked is controlled by the 'sink.rolling-policy.check-interval' option.");

    public static final ConfigOption<Long> SINK_HIVE_ROLLING_POLICY_CHECK_INTERVAL =
            key("sink.hive.rolling-policy.check-interval")
                    .defaultValue(Duration.ofMinutes(1).toMillis())
                    .withDescription("The interval for checking time based rolling policies. "
                            + "This controls the frequency to check whether a part file should rollover based on"
                            + " 'sink.rolling-policy.rollover-interval'.");

    public static final ConfigOption<Integer> SINK_HIVE_TEXT_BUFFER_SIZE = key("sink.hive.text.buffer.size")
            .defaultValue(262144)
            .withDescription("Buffer size of Hive/THive sink text format (with compression or not), "
                    + "default size is 256KB");

    // ------------------------------------------------------------------------
    // Checkpoint related configs
    // ------------------------------------------------------------------------
    public static final ConfigOption<Integer> CHECKPOINT_INTERVAL_MS = key("checkpoint.interval")
            .defaultValue(600000)
            .withDescription("The interval between tow checkpoints");

    public static final ConfigOption<Integer> MIN_PAUSE_BETWEEN_CHECKPOINTS_MS = key("min.pause.between.checkpoints.ms")
            .defaultValue(500);

    public static final ConfigOption<Integer> CHECKPOINT_TIMEOUT_MS =
            key("checkpoint.timeout.ms").defaultValue(600000);

    // ------------------------------------------------------------------------
    // Metrics related
    // ------------------------------------------------------------------------
    public static final ConfigOption<Boolean> METRICS_ENABLE_OUTPUT =
            key("metrics.enable.output").defaultValue(true);

    public static final ConfigOption<Integer> METRICS_TIMESTAMP_WATERMARK_ASSIGNER_PARALLELISM =
            key("metrics.timestamp.watermark.assigner.parallelism").defaultValue(1);

    public static final ConfigOption<Integer> METRICS_AGGREGATOR_PARALLELISM =
            key("metrics.aggregator.parallelism").defaultValue(1);

    public static final ConfigOption<Integer> METRICS_SINK_PARALLELISM =
            key("metrics.sink.parallelism").defaultValue(1)
                    .withDeprecatedKeys("metrics.mysql.sink.parallelism");

    public static final String METRICS_TIMESTAMP_AND_WATERMARK_ASSIGNER_UID =
            "metrics_timestamp_and_watermark_assigner_uid";

    public static final String METRICS_AGGREGATOR_UID = "metrics_aggregator_uid";

    public static final String METRICS_SINK_UID = "metrics_sink_uid";

    public static final ConfigOption<Integer> METRICS_AGGREGATOR_WINDOW_SIZE = key("metrics.aggregator.window.size")
            .defaultValue(5)
            .withDescription("minutes");

    public static final ConfigOption<String> METRICS_LABELS =
            ConfigOptions.key("inlong.metric.labels")
                    .noDefaultValue()
                    .withDescription("INLONG metric labels, format is 'key1=value1&key2=value2',"
                            + "default is 'groupId=xxx&streamId=xxx&nodeId=xxx'");

    public static final ConfigOption<String> METRICS_AUDIT_PROXY_HOSTS =
            ConfigOptions.key("metrics.audit.proxy.hosts")
                    .noDefaultValue()
                    .withDescription("Audit proxy host address for reporting audit metrics. \n"
                            + "e.g. 127.0.0.1:10081,0.0.0.1:10081");

    // ------------------------------------------------------------------------
    // Single tenant related
    // ------------------------------------------------------------------------
    public static final ConfigOption<String> DATAFLOW_INFO_FILE = key("dataflow.info.file").noDefaultValue()
            .withDescription("The file which contains dataflow info for a single tenant job");

    public static final ConfigOption<Boolean> JOB_ORDERLY_OUTPUT = key("job.orderly.output").defaultValue(false)
            .withDescription("Whether to ensure orderly output or not");
    public static final ConfigOption<Integer> ORC_SINK_BATCH_SIZE =
            key(HIVE_SINK_PREFIX + "orc.row.batch.size").defaultValue(64);

    public static final String CHDFS_CONFIG_PREFIX = "fs.ofs.";

    public static final ConfigOption<Boolean> LIGHTWEIGHT = key("lightweight").defaultValue(false)
            .withDescription("Whether to lightweight or not");

    public static final ConfigOption<String> GROUP_INFO_FILE = key("group.info.file").noDefaultValue()
            .withDescription("The file which contains group info for a single tenant job");

    public static final ConfigOption<String> SQL_SCRIPT_FILE = key("sql.script.file").noDefaultValue()
            .withDescription("The file which is sql script and contains multi statement");

    // ------------------------------------------------------------------------
    // File format and compression related
    // ------------------------------------------------------------------------
    public enum CompressionType {
        NONE,
        GZIP,
        LZO
    }

}
