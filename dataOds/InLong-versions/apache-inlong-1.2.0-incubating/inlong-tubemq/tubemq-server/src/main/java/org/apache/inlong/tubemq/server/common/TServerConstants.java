/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.tubemq.server.common;

import org.apache.inlong.tubemq.corebase.TBaseConstants;
import org.apache.inlong.tubemq.server.broker.utils.DataStoreUtils;

public final class TServerConstants {

    public static final String TOKEN_JOB_TOPICS = "topics";
    public static final String TOKEN_JOB_STORE_MGR = "messageStoreManager";
    public static final String TOKEN_DEFAULT_FLOW_CONTROL = "default_master_ctrl";

    public static final long DEFAULT_DATA_VERSION = 0L;

    public static final String BLANK_FLOWCTRL_RULES = "[]";
    public static final String BLANK_FILTER_ITEM_STR = ",,";

    public static final int QRY_PRIORITY_DEF_VALUE = 301;
    public static final int QRY_PRIORITY_MIN_VALUE = 101;
    public static final int QRY_PRIORITY_MAX_VALUE = 303;

    public static final int TOPIC_STOREBLOCK_NUM_MIN = 1;

    public static final int TOPIC_PARTITION_NUM_MIN = 1;

    public static final int TOPIC_DSK_UNFLUSHTHRESHOLD_MIN = 0;
    public static final int TOPIC_DSK_UNFLUSHTHRESHOLD_DEF = 1000;

    public static final int TOPIC_DSK_UNFLUSHINTERVAL_MIN = 1;
    public static final int TOPIC_DSK_UNFLUSHINTERVAL_DEF = 10000;

    public static final int TOPIC_DSK_UNFLUSHDATAHOLD_MIN = 0;

    public static final int TOPIC_CACHESIZE_MB_MIN = 2;
    public static final int TOPIC_CACHESIZE_MB_DEF = 3;
    public static final int TOPIC_CACHESIZE_MB_MAX = 2048;

    public static final int TOPIC_CACHEINTVL_MIN = 4000;
    public static final int TOPIC_CACHEINTVL_DEF = 20000;

    public static final int TOPIC_CACHECNT_INK_MIN = 1;
    public static final int TOPIC_CACHECNT_INK_DEF = 10;

    public static final String TOPIC_POLICY_DEF = "delete,168h";

    public static final int TOPIC_RET_PERIOD_IN_SEC_DEF = 14515200;

    public static final int GROUP_BROKER_CLIENT_RATE_MIN = 0;

    public static final int BROKER_REGION_ID_MIN = 0;
    public static final int BROKER_REGION_ID_DEF = 0;

    public static final int BROKER_GROUP_ID_MIN = 0;
    public static final int BROKER_GROUP_ID_DEF = 0;

    public static final int TOPIC_ID_MIN = 0;
    public static final int TOPIC_ID_DEF = 0;

    public static final int DATA_VERSION_ID_MIN = 0;
    public static final int DATA_VERSION_ID_DEF = 0;

    public static final int CFG_MODAUTHTOKEN_MAX_LENGTH = 128;
    public static final int CFG_ROWLOCK_DEFAULT_DURATION = 30000;
    public static final int CFG_ZK_COMMIT_DEFAULT_RETRIES = 10;
    public static final int CFG_STORE_DEFAULT_MSG_READ_UNIT = 327680;
    public static final int CFG_BATCH_BROKER_OPERATE_MAX_COUNT = 50;
    public static final int CFG_BATCH_RECORD_OPERATE_MAX_COUNT = 100;

    public static final int CFG_DEFAULT_DATA_UNFLUSH_HOLD = 0;
    public static final int CFG_DEFAULT_CONSUME_RULE = 300;
    public static final int CFG_DELETEWHEN_MAX_LENGTH = 1024;
    public static final int CFG_DELETEPOLICY_MAX_LENGTH = 1024;
    public static final int CFG_CONSUMER_CLIENTID_MAX_LENGTH =
            TBaseConstants.META_MAX_GROUPNAME_LENGTH + 512;

    public static final long CFG_REPORT_DEFAULT_SYNC_DURATION = 2 * 3600 * 1000;
    public static final long CFG_STORE_STATS_MAX_REFRESH_DURATION = 20 * 60 * 1000;

    public static final String OFFSET_HISTORY_NAME = "__offset_history__";
    public static final String TOKEN_OFFSET_GROUP = "$groupName$";
    public static final int OFFSET_HISTORY_NUMSTORES = 1;
    public static final int OFFSET_HISTORY_NUMPARTS = 10;
    public static final long CFG_DEFAULT_GROUP_OFFSET_SCAN_DUR = 60000L;
    public static final long CFG_MIN_GROUP_OFFSET_SCAN_DUR = 30000L;
    public static final long CFG_MAX_GROUP_OFFSET_SCAN_DUR = 480000L;

    public static final long CFG_OFFSET_RESET_MIN_ALARM_CHECK =
            DataStoreUtils.STORE_INDEX_HEAD_LEN * 100000L;
    public static final long CFG_OFFSET_RESET_MID_ALARM_CHECK =
            DataStoreUtils.STORE_INDEX_HEAD_LEN * 1000000L;

    // max statistics token type length
    public static final int META_MAX_STATSTYPE_LENGTH = 256;
}
