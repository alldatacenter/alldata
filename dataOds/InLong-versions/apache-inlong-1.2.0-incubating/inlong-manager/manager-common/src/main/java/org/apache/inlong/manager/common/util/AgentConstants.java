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

package org.apache.inlong.manager.common.util;

/**
 * Agent constants.
 */
public class AgentConstants {
    public static String NAME = "name";
    public static String IP = "ip";
    public static String MAX_RETRY_THREADS = "max_retry_threads";
    public static String MIN_RETRY_THREADS = "min_retry_threads";
    public static String EVENT_CHECK_INTERVAL = "event_check_interval";
    public static String DB_PATH = "db_path";

    public static String SCAN_INTERVAL_SEC = "scan_interval_sec";

    public static String BACK_AGENT_CONF_FILE = "back_agent_conf_file";
    public static String FILECONTENT_AGENT_CONF_FILE = "filecontent_agent_conf_file";

    public static String BATCH_SIZE = "batch_size";
    public static String MSG_SIZE = "msg_size";
    public static String SEND_RUNNABLEC_SIZE = "send_runnable_size";
    public static String ONELINE_SIZE = "oneline_size";

    public static String MSG_QUEUE_SIZE = "msg_queue_size";
    public static String CLEAR_DAY_OFFSET = "clear_day_offset";
    public static String CLEAR_INTERVAL_SEC = "clear_interval_sec";

    public static String MAX_CONCURRENT_READER_PER_TASK = "max_concurrent_reader_per_task";
    public static String MAX_READER_CNT = "max_reader_cnt";
    public static String THREAD_MANAGER_SLEEP_INTERVAL = "thread_manager_sleep_interval";
    public static String BUFFER_SIZE_IN_BYTES = "buffer_size_in_bytes";
    public static String CONF_REFRESH_INTERVAL_SECS = "conf_refresh_interval_secs";

    public static String RPC_CLIENT_RECONNECT = "agent_rpc_reconnect_time";

    public static String COMPRESS = "compress";
    public static String IS_SEND = "is_send";
    public static String BUFFER_SIZE = "buffer_size";
    public static String IS_PACKAGE = "is_package";
    public static String IS_WRITE = "is_write";
    public static String IS_CALC_MD5 = "is_calc_md5";
    public static String SEND_TIMEOUT_MILL_SEC = "send_timeout_mill_sec";
    public static String FLUSH_EVENT_TIMEOUT_MILL_SEC = "flush_event_timeout_mill_sec";

    public static String STAT_INTERVAL_SEC = "stat_interval_sec";

    public static String FLOW_SIZE = "flow_size";

    public static boolean DEFAULT_BCOMPRESS = true;
    public static boolean DEFAULT_IS_WRITE = true;
    public static boolean DEFAULT_IS_CALC_MD5 = true;
    public static boolean DEFAULT_BPACKAGE = true;
    public static boolean DEFAULT_IS_SEND = true;
}
