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

import java.util.Arrays;
import java.util.List;

public class TErrCodeConstants {
    public static final int SUCCESS = 200;
    public static final int NOT_READY = 201;
    public static final int MOVED = 301;

    public static final int BAD_REQUEST = 400;
    public static final int UNAUTHORIZED = 401;
    public static final int CONNECT_RETURN_NULL = 402;
    public static final int FORBIDDEN = 403;
    public static final int NOT_FOUND = 404;
    public static final int ALL_PARTITION_FROZEN = 405;
    public static final int NO_PARTITION_ASSIGNED = 406;
    public static final int ALL_PARTITION_WAITING = 407;
    public static final int ALL_PARTITION_INUSE = 408;
    public static final int PARTITION_OCCUPIED = 410;
    public static final int HB_NO_NODE = 411;
    public static final int DUPLICATE_PARTITION = 412;
    public static final int CERTIFICATE_FAILURE = 415;
    public static final int SERVER_RECEIVE_OVERFLOW = 419;
    public static final int CLIENT_SHUTDOWN = 420;
    public static final int CLIENT_HIGH_FREQUENCY_REQUEST = 421;
    public static final int PARTITION_UNSUBSCRIBABLE = 422;
    public static final int PARTITION_UNPUBLISHABLE = 423;
    public static final int CLIENT_INCONSISTENT_CONSUMETYPE = 424;
    public static final int CLIENT_INCONSISTENT_TOPICSET = 425;
    public static final int CLIENT_INCONSISTENT_FILTERSET = 426;
    public static final int CLIENT_INCONSISTENT_SESSIONKEY = 427;
    public static final int CLIENT_INCONSISTENT_SELECTBIG = 428;
    public static final int CLIENT_INCONSISTENT_SOURCECOUNT = 429;
    public static final int CLIENT_DUPLICATE_INDEXID = 430;

    public static final int CONSUME_GROUP_FORBIDDEN = 450;
    public static final int SERVER_CONSUME_SPEED_LIMIT = 452;
    public static final int CONSUME_CONTENT_FORBIDDEN = 455;

    public static final int INTERNAL_SERVER_ERROR = 500;
    public static final int SERVICE_UNAVAILABLE = 503;
    public static final int INTERNAL_SERVER_ERROR_MSGSET_NULL = 510;
    public static final int UNSPECIFIED_ABNORMAL = 599;

    public static final List<Integer> IGNORE_ERROR_SET =
            Arrays.asList(BAD_REQUEST, NOT_FOUND, ALL_PARTITION_FROZEN,
                    NO_PARTITION_ASSIGNED, ALL_PARTITION_WAITING, ALL_PARTITION_INUSE);
}
