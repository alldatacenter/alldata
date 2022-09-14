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

package org.apache.inlong.agent.constant;

/**
 * Basic config for a single job
 */
public class JobConstants extends CommonConstants {

    // job id
    public static final String JOB_ID = "job.id";
    public static final String JOB_INSTANCE_ID = "job.instance.id";
    public static final String JOB_IP = "job.ip";
    public static final String JOB_RETRY = "job.retry";
    public static final String JOB_UUID = "job.uuid";
    public static final String JOB_GROUP_ID = "job.groupId";
    public static final String JOB_STREAM_ID = "job.streamId";

    public static final String JOB_SOURCE_CLASS = "job.source";
    public static final String JOB_SOURCE_TYPE = "job.sourceType";

    public static final String JOB_SINK = "job.sink";
    public static final String JOB_CHANNEL = "job.channel";
    public static final String JOB_NAME = "job.name";
    public static final String JOB_LINE_FILTER_PATTERN = "job.pattern";

    public static final String DEFAULT_JOB_NAME = "default";
    public static final String JOB_DESCRIPTION = "job.description";
    public static final String DEFAULT_JOB_DESCRIPTION = "default job description";
    public static final String DEFAULT_JOB_LINE_FILTER = "";

    //File job
    public static final String JOB_TRIGGER = "job.fileJob.trigger";
    public static final String JOB_DIR_FILTER_PATTERN = "job.fileJob.dir.pattern";
    public static final String JOB_FILE_TIME_OFFSET = "job.fileJob.timeOffset";
    public static final String JOB_FILE_MAX_WAIT = "job.fileJob.file.max.wait";
    public static final String JOB_CYCLE_UNIT = "job.fileJob.cycleUnit";
    public static final String JOB_FILE_COLLECT_TYPE = "job.fileJob.collectType";
    public static final String JOB_FILE_LINE_END_PATTERN = "job.fileJob.line.endPattern";
    public static final String JOB_FILE_CONTENT_COLLECT_TYPE = "job.fileJob.contentCollectType";
    public static final String JOB_FILE_META_ENV_LIST = "job.fileJob.envList";
    public static final String JOB_FILE_META_FILTER_BY_LABELS = "job.fileJob.filterMetaByLabels";
    public static final String JOB_FILE_PROPERTIES = "job.fileJob.properties";
    public static final String JOB_FILE_DATA_SOURCE_COLUMN_SEPARATOR = "job.fileJob.dataSeparator";
    public static final String JOB_FILE_MONITOR_INTERVAL = "job.fileJob.monitorInterval";
    public static final String JOB_FILE_MONITOR_STATUS = "job.fileJob.monitorStatus";
    public static final String JOB_FILE_MONITOR_EXPIRE = "job.fileJob.monitorExpire";

    //Binlog job
    public static final String JOB_DATABASE_USER = "job.binlogJob.user";
    public static final String JOB_DATABASE_PASSWORD = "job.binlogJob.password";
    public static final String JOB_DATABASE_HOSTNAME = "job.binlogJob.hostname";
    public static final String JOB_TABLE_WHITELIST = "job.binlogJob.tableWhiteList";
    public static final String JOB_DATABASE_WHITELIST = "job.binlogJob.databaseWhiteList";
    public static final String JOB_DATABASE_OFFSETS = "job.binlogJob.offsets";
    public static final String JOB_DATABASE_OFFSET_FILENAME = "job.binlogJob.offset.filename";

    public static final String JOB_DATABASE_SERVER_TIME_ZONE = "job.binlogJob.serverTimezone";
    public static final String JOB_DATABASE_STORE_OFFSET_INTERVAL_MS = "job.binlogJob.offset.intervalMs";

    public static final String JOB_DATABASE_STORE_HISTORY_FILENAME = "job.binlogJob.history.filename";
    public static final String JOB_DATABASE_INCLUDE_SCHEMA_CHANGES = "job.binlogJob.schema";
    public static final String JOB_DATABASE_SNAPSHOT_MODE = "job.binlogJob.snapshot.mode";
    public static final String JOB_DATABASE_HISTORY_MONITOR_DDL = "job.binlogJob.ddl";
    public static final String JOB_DATABASE_PORT = "job.binlogJob.port";

    //Kafka job
    public static final String JOB_KAFKA_TOPIC = "job.kafkaJob.topic";
    public static final String JOB_KAFKA_BOOTSTRAP_SERVERS = "job.kafkaJob.bootstrap.servers";
    public static final String JOB_KAFKA_GROUP_ID = "job.kafkaJob.group.id";
    public static final String JOB_KAFKA_RECORD_SPEED_LIMIT = "job.kafkaJob.recordSpeed.limit";
    public static final String JOB_KAFKA_BYTE_SPEED_LIMIT = "job.kafkaJob.byteSpeed.limit";
    public static final String JOB_KAFKA_OFFSET = "job.kafkaJob.partition.offset";
    public static final String JOB_KAFKA_READ_TIMEOUT = "job.kafkaJob.read.timeout";
    public static final String JOB_KAFKA_AUTO_COMMIT_OFFSET_RESET = "job.kafkaJob.autoOffsetReset";

    public static final Long JOB_KAFKA_DEFAULT_OFFSET = 0L;

    // job type, delete/add
    public static final String JOB_TYPE = "job.type";

    public static final String JOB_CHECKPOINT = "job.checkpoint";

    public static final String DEFAULT_JOB_FILE_TIME_OFFSET = "0d";

    // time in min
    public static final int DEFAULT_JOB_FILE_MAX_WAIT = 1;

    public static final String JOB_READ_WAIT_TIMEOUT = "job.file.read.wait";

    public static final int DEFAULT_JOB_READ_WAIT_TIMEOUT = 100;

    public static final String JOB_ID_PREFIX = "job_";

    public static final String SQL_JOB_ID = "sql_job_id";

    public static final String JOB_STORE_TIME = "job.store.time";

    public static final String JOB_OP = "job.op";

    public static final String TRIGGER_ONLY_ONE_JOB = "job.standalone";

    // field splitter
    public static final String JOB_FIELD_SPLITTER = "job.splitter";

    // job delivery time
    public static final String JOB_DELIVERY_TIME = "job.deliveryTime";

    // job time reading file
    public static final String JOB_DATA_TIME = "job.dataTime";

    /**
     * when job is retried, the retry time should be provided
     */
    public static final String JOB_RETRY_TIME = "job.retryTime";

    /**
     * delimiter to split offset for different task
     */
    public static final String JOB_OFFSET_DELIMITER = "_";

    /**
     * delimiter to split all partition offset for all kafka tasks
     */
    public static final String JOB_KAFKA_PARTITION_OFFSET_DELIMITER = "#";

    /**
     * sync send data when sending to DataProxy
     */
    public static final int SYNC_SEND_OPEN = 1;

    public static final String INTERVAL_MILLISECONDS = "500";

    /**
     * monitor switch, 1 true and 0 false
     */
    public static final String JOB_FILE_MONITOR_DEFAULT_STATUS = "1";

    /**
     * monitor expire time and the time in milliseconds.
     * default value is -1 and stand for not expire time.
     */
    public static final String JOB_FILE_MONITOR_DEFAULT_EXPIRE = "-1";


}
