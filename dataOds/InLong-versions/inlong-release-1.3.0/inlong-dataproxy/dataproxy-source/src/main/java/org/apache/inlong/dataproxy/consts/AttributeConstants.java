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

package org.apache.inlong.dataproxy.consts;

/**
 * Attribute constants
 */
public class AttributeConstants {

    public static final String SEPARATOR = "&";
    public static final String KEY_VALUE_SEPARATOR = "=";
    public static final String SEP_HASHTAG = "#";

    public static final String BODY = "body";
    public static final String CHARSET = "UTF-8";
    public static final String HTTP_REQUEST = "http-request";
    public static final String HTTP_RESPONSE = "http-response";

    /**
     * group id unique string id for each business or product
     */
    public static final String GROUP_ID = "groupId";

    /**
     * interface id unique string id for each interface of business An interface stand for a kind of
     * data
     */
    public static final String STREAM_ID = "streamId";

    /**
     * iname is like a streamId but used in file protocol(m=xxx)
     */
    public static final String INAME = "iname";

    /**
     * data time
     */
    public static final String DATA_TIME = "dt";

    public static final String TIME_STAMP = "t";

    /* compress type */
    public static final String COMPRESS_TYPE = "cp";

    /* count value for how many records a message body contains */
    public static final String MESSAGE_COUNT = "cnt";

    /* message type */
    public static final String MESSAGE_TYPE = "mt";

    /* sort type */
    public static final String METHOD = "m";

    /* global unique id for a message*/
    public static final String SEQUENCE_ID = "sid";

    public static final String UNIQ_ID = "uniq";

    /* from where */
    public static final String FROM = "f";

    public static final String RCV_TIME = "rt";

    public static final String NODE_IP = "NodeIP";

    public static final String NUM2NAME = "num2name";

    public static final String GROUPID_NUM = "groupIdnum";

    public static final String STREAMID_NUM = "streamIdnum";

    public static final String MESSAGE_PARTITION_KEY = "partitionKey";

    public static final String MESSAGE_SYNC_SEND = "syncSend";

    public static final String MESSAGE_IS_ACK = "isAck";

}
