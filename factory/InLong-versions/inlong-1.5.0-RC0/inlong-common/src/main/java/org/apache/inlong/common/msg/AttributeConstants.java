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

package org.apache.inlong.common.msg;

public interface AttributeConstants {

    String SEPARATOR = "&";
    String KEY_VALUE_SEPARATOR = "=";

    /**
     * group id
     * unique string id for each business or product
     */
    String GROUP_ID = "groupId";

    /**
     * stream id
     * unique string id for each interface of business
     * An steam stand for a kind of data
     */
    String STREAM_ID = "streamId";

    /**
     * iname is like a streamId but used in file protocol(m=xxx)
     */
    String INAME = "iname";

    /**
     * data time
     */
    String DATA_TIME = "dt";
    String TIME_STAMP = "t";

    /* compress type */
    String COMPRESS_TYPE = "cp";

    /* count value for how many records a message body contains */
    String MESSAGE_COUNT = "cnt";

    /* message type */
    String MESSAGE_TYPE = "mt";

    /* sort type */
    String METHOD = "m";

    /* global unique id for a message */
    String SEQUENCE_ID = "sid";

    String UNIQ_ID = "uniq";

    /* from where */
    String FROM = "f";

    // whether to return a response, false: not need, true or not exist: need
    String MESSAGE_IS_ACK = "isAck";

    // whether sync send message
    String MESSAGE_SYNC_SEND = "syncSend";

    // whether sent by partition key, use with MESSAGE_SYNC_SEND
    String MESSAGE_PARTITION_KEY = "partitionKey";

    // whether return response on sink
    String MESSAGE_PROXY_SEND = "proxySend";

    // message received time, add by receiver
    String RCV_TIME = "rt";

    // message received node ip, add by receiver
    String NODE_IP = "NodeIP";

    // error code, add by receiver
    String MESSAGE_PROCESS_ERRCODE = "errCode";

    // error message, add by receiver
    String MESSAGE_PROCESS_ERRMSG = "errMsg";

    String MESSAGE_ID = "messageId";

    // dataproxy IP from dp response ack
    String MESSAGE_DP_IP = "dpIP";

    String MESSAGE_TOPIC = "topic";

    // dataproxy IP, used in trace info
    String DATAPROXY_NODE_IP = "node2ip";

    // dataproxy received time, used in trace info
    String DATAPROXY_RCVTIME = "rtime2";

    // Message reporting time, in milliseconds
    // Provided by the initial sender of the data, and passed to
    // the downstream by the Bus without modification for the downstream to
    // calculate the end-to-end message delay; if this field does not exist in the request,
    // it will be added by the Bus with the current time
    public static final String MSG_RPT_TIME = "rtms";
}
