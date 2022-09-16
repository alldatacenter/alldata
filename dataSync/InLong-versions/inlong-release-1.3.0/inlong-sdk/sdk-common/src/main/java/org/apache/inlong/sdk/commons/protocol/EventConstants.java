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

package org.apache.inlong.sdk.commons.protocol;

/**
 * 
 * EventConstants
 */
public interface EventConstants {
    String HEADER_KEY_VERSION = "version";
    String HEADER_SDK_VERSION_1 = "1";
    String HEADER_CACHE_VERSION_1 = "1";
    // sdk
    String INLONG_GROUP_ID = "inlongGroupId";
    String INLONG_STREAM_ID = "inlongStreamId";
    String HEADER_KEY_MSG_TIME = "msgTime";
    String HEADER_KEY_SOURCE_IP = "sourceIp";
    // proxy
    String HEADER_KEY_SOURCE_TIME = "sourceTime";
    String TOPIC = "topic";
    String HEADER_KEY_PROXY_NAME = "proxyName";
    String HEADER_KEY_PACK_TIME = "packTime";
    String HEADER_KEY_MSG_COUNT = "msgCount";
    String HEADER_KEY_SRC_LENGTH = "srcLength";
    String HEADER_KEY_COMPRESS_TYPE = "compressType";
    // sort
    String HEADER_KEY_MESSAGE_KEY = "messageKey";
    String HEADER_KEY_MSG_OFFSET = "msgOffset";
    // other
    String RELOAD_INTERVAL = "reloadInterval";
}
