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

package org.apache.inlong.manager.common.consts;

import org.apache.inlong.common.enums.DataProxyMsgEncType;

import com.google.common.collect.Sets;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Global constant for the Inlong system.
 */
public class InlongConstants {

    /**
     * Thread pool related config.
     */
    public static final int CORE_POOL_SIZE = 10;
    public static final int MAX_POOL_SIZE = 20;
    public static final long ALIVE_TIME_MS = 100L;
    public static final int QUEUE_SIZE = 10000;

    /**
     * Group config
     */
    public static final String COMMA = ",";

    public static final String DOT = ".";

    public static final String BLANK = " ";

    public static final String SLASH = "/";

    public static final String COLON = ":";

    public static final String EQUAL = "=";

    public static final String SEMICOLON = ";";

    public static final String HYPHEN = "-";

    public static final String UNDERSCORE = "_";

    public static final String LEFT_BRACKET = "(";

    public static final String PERCENT = "%";

    public static final String QUESTION_MARK = "?";

    public static final String NEW_LINE = "\n";

    public static final String ADMIN_USER = "admin";

    public static final Integer AFFECTED_ONE_ROW = 1;

    public static final Integer INITIAL_VERSION = 1;

    public static final Integer UN_DELETED = 0;

    public static final Integer DELETED_STATUS = 10;

    public static final Integer STANDARD_MODE = 0;
    public static final Integer DATASYNC_MODE = 1;

    public static final Integer DISABLE_ZK = 0;
    public static final Integer ENABLE_ZK = 1;

    public static final Integer DISABLE_CREATE_RESOURCE = 0;
    public static final Integer ENABLE_CREATE_RESOURCE = 1;

    /**
     * Data report type, support:
     * <pre>
     *     0: report to DataProxy and respond when the DataProxy received data.
     *     1: report to DataProxy and respond after DataProxy sends data.
     *     2: report to MQ and respond when the MQ received data.
     * </pre>
     */
    public static final Integer REPORT_TO_DP_RECEIVED = 0;
    public static final Integer REPORT_TO_DP_SENT = 1;
    public static final Integer REPORT_TO_MQ_RECEIVED = 2;

    public static final Integer UN_SYNC_SEND = 0;
    public static final Integer SYNC_SEND = 1;

    public static final String BATCH_TASK = "batch.task";

    /**
     * Pulsar config
     */
    public static final String PULSAR_AUTHENTICATION = "pulsar.authentication";

    public static final String PULSAR_AUTHENTICATION_TYPE = "pulsar.authentication.type";

    public static final String DEFAULT_PULSAR_TENANT = "public";

    public static final String DEFAULT_PULSAR_AUTHENTICATION_TYPE = "token";

    public static final String PULSAR_QUEUE_TYPE_SERIAL = "SERIAL";

    public static final String PULSAR_QUEUE_TYPE_PARALLEL = "PARALLEL";

    /**
     * Format of the Pulsar topic: "persistent://tenant/namespace/topic
     */
    public static final String PULSAR_TOPIC_FORMAT = "persistent://%s/%s/%s";

    /**
     * Sort config
     */
    public static final String DATAFLOW = "dataflow";

    public static final String STREAMS = "streams";

    public static final String RELATIONS = "relations";

    public static final String INPUTS = "inputs";

    public static final String OUTPUTS = "outputs";

    public static final String NODES = "nodes";

    public static final String NODE_TYPE = "type";

    public static final String LOAD = "Load";

    public static final String EXTRACT = "Extract";

    public static final String SORT_JOB_ID = "sort.job.id";

    public static final String SORT_TYPE = "sort.type";

    public static final String DEFAULT_SORT_TYPE = "flink";

    public static final String SORT_NAME = "sort.name";

    public static final String SORT_URL = "sort.url";

    public static final String SORT_AUTHENTICATION = "sort.authentication";

    public static final String SORT_AUTHENTICATION_TYPE = "sort.authentication.type";

    public static final String DEFAULT_SORT_AUTHENTICATION_TYPE = "secret_and_token";

    public static final String SORT_PROPERTIES = "sort.properties";

    public static final String DATA_TYPE_RAW_PREFIX = "RAW_";

    public static final int DEFAULT_ENABLE_VALUE = 1;

    public static final String STATEMENT_TYPE_SQL = "sql";
    public static final String STATEMENT_TYPE_JSON = "json";
    public static final String STATEMENT_TYPE_CSV = "csv";

    public static final String SORT_TYPE_INFO_SUFFIX = "TypeInfo";

    public static final Pattern PATTERN_NORMAL_CHARACTERS = Pattern.compile("^[a-zA-Z0-9_]*$");

    public static final Set<String> STREAM_FIELD_TYPES =
            Sets.newHashSet("string", "int", "long", "float", "double", "date", "timestamp");

    /**
     * The name prop when batch parsing fields in JSON mode
     */
    public static final String BATCH_PARSING_FILED_JSON_NAME_PROP = "name";

    /**
     * The type prop when batch parsing fields in JSON mode
     */
    public static final String BATCH_PARSING_FILED_JSON_TYPE_PROP = "type";

    /**
     * The comment prop when batch parsing fields in JSON mode
     */
    public static final String BATCH_PARSING_FILED_JSON_COMMENT_PROP = "desc";

    /**
     * Message compression type, 0: Raw message without any InLong format, 1: InlongMsgPb, 2: InlongMsg
     * <p/>
     * See more: {@link DataProxyMsgEncType}
     */
    public static final String MSG_ENCODE_VER = "msgEnType";

}
