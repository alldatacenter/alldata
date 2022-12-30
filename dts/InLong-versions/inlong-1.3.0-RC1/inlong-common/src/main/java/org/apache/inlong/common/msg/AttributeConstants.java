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
     * interface id
     * unique string id for each interface of business
     * An interface stand for a kind of data
     */
    String INTERFACE_ID = "streamId";

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

    /* global unique id for a message*/
    String SEQUENCE_ID = "sid";

    String UNIQ_ID = "uniq";

    /* from where */
    String FROM = "f";

    String RCV_TIME = "rt";

    String NODE_IP = "NodeIP";

}
