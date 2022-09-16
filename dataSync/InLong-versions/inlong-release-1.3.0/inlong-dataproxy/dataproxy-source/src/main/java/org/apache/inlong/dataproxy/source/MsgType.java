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

package org.apache.inlong.dataproxy.source;

public enum MsgType {
    /**
     * heartbeat
     */
    MSG_HEARTBEAT(1),
    MSG_COMMON_SERVICE(2),
    MSG_ACK_SERVICE(3),
    MSG_ORIGINAL_RETURN(4),
    MSG_MULTI_BODY(5),
    MSG_MULTI_BODY_ATTR(6),
    MSG_BIN_MULTI_BODY(7),
    MSG_BIN_HEARTBEAT(8),
    MSG_UNKNOWN(-1);

    private final int value;

    MsgType(int value) {
        this.value = value;
    }

    public static MsgType valueOf(int type) {
        int inputType = (type & 0x1F);

        for (MsgType msgType : MsgType.values()) {
            if (msgType.getValue() == inputType) {
                return msgType;
            }
        }
        return MSG_UNKNOWN;
    }

    public int getValue() {
        return value;
    }
}
