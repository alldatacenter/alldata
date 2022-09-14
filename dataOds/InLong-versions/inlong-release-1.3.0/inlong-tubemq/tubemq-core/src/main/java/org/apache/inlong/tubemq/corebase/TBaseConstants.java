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

package org.apache.inlong.tubemq.corebase;

public class TBaseConstants {

    public static final int META_VALUE_UNDEFINED = -2;

    public static final int BUILDER_DEFAULT_SIZE = 512;

    public static final int META_DEFAULT_MASTER_PORT = 8715;
    public static final int META_DEFAULT_MASTER_TLS_PORT = 8716;
    public static final int META_DEFAULT_BROKER_PORT = 8123;
    public static final int META_DEFAULT_BROKER_TLS_PORT = 8124;
    public static final int META_DEFAULT_BROKER_WEB_PORT = 8081;
    public static final int META_STORE_INS_BASE = 10000;

    public static final String META_DEFAULT_CHARSET_NAME = "UTF-8";
    public static final int META_MAX_MSGTYPE_LENGTH = 255;
    public static final int META_MAX_PARTITION_COUNT = 100;
    public static final int META_MAX_BROKER_IP_LENGTH = 32;
    public static final int META_MAX_USERNAME_LENGTH = 64;
    public static final int META_MAX_CALLBACK_STRING_LENGTH = 128;
    public static final int META_MAX_TOPICNAME_LENGTH = 64;
    public static final int META_MAX_GROUPNAME_LENGTH = 1024;
    public static final int META_MAX_OPREASON_LENGTH = 1024;

    public static final String META_TMP_FILTER_VALUE = "^[_A-Za-z0-9]+$";
    public static final String META_TMP_STRING_VALUE = "^[a-zA-Z]\\w+$";
    public static final String META_TMP_NUMBER_VALUE = "^-?[0-9]\\d*$";
    public static final String META_TMP_GROUP_VALUE = "^[a-zA-Z][\\w-]+$";
    public static final String META_TMP_CONSUMERID_VALUE = "^[_A-Za-z0-9\\.\\-]+$";
    public static final String META_TMP_CALLBACK_STRING_VALUE = "^[_A-Za-z0-9]+$";
    public static final String META_TMP_IP_ADDRESS_VALUE =
            "((?:(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d)))\\.){3}(?:25[0-5]|2[0-4]\\d|((1\\d{2})|([1-9]?\\d))))";
    public static final String META_TMP_PORT_REGEX = "[\\d]+";

    public static final int CONSUME_MODEL_READ_NORMAL = 0;
    public static final int CONSUME_MODEL_READ_FROM_MAX = 1;
    public static final int CONSUME_MODEL_READ_FROM_MAX_ALWAYS = 2;

    public static final long CFG_FC_MAX_LIMITING_DURATION = 60000;
    public static final long CFG_FC_MAX_SAMPLING_PERIOD = 5000;
    public static final int CFG_FLT_MAX_FILTER_ITEM_LENGTH = 256;
    public static final int CFG_FLT_MAX_FILTER_ITEM_COUNT = 500;

    public static final int META_MAX_CLIENT_HOSTNAME_LENGTH = 256;
    public static final int META_MAX_CLIENT_ID_LENGTH = 1024;
    public static final int META_MAX_BOOKED_TOPIC_COUNT = 1024;

    public static final long CFG_DEFAULT_AUTH_TIMESTAMP_VALID_INTERVAL = 20000;

    public static final int META_MB_UNIT_SIZE = (1024 * 1024);
    public static final int META_MESSAGE_SIZE_ADJUST = (512 * 1024);
    public static final int META_MAX_MESSAGE_HEADER_SIZE = (10 * 1024);

    public static final int META_MIN_ALLOWED_MESSAGE_SIZE_MB = 1;
    public static final int META_MAX_ALLOWED_MESSAGE_SIZE_MB = 20;
    public static final int META_MAX_MESSAGE_DATA_SIZE =
            META_MIN_ALLOWED_MESSAGE_SIZE_MB * META_MB_UNIT_SIZE;
    public static final int META_MIN_MEM_BUFFER_SIZE =
            META_MAX_MESSAGE_DATA_SIZE + META_MESSAGE_SIZE_ADJUST;
    public static final int META_MAX_MESSAGE_DATA_SIZE_UPPER_LIMIT =
            META_MAX_ALLOWED_MESSAGE_SIZE_MB * META_MB_UNIT_SIZE;

    public static final long INDEX_MSG_UNIT_SIZE = 28;

    public static final long CFG_DEF_META_FORCE_UPDATE_PERIOD = 3 * 60 * 1000;
    public static final long CFG_MIN_META_FORCE_UPDATE_PERIOD = 1 * 60 * 1000;
    public static final long CFG_STATS_MIN_SNAPSHOT_PERIOD_MS = 2000;

}
