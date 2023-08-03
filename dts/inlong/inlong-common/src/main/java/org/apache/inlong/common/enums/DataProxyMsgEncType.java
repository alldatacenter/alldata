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

package org.apache.inlong.common.enums;

/**
 * Enumeration class of encoding format of data output from DataProxy to MQ
 */
public enum DataProxyMsgEncType {

    MSG_ENCODE_TYPE_RAW(0, "Raw", "Raw message without any InLong format"),
    MSG_ENCODE_TYPE_PB(1, "PB", "The PB MessagePack encode format"),
    MSG_ENCODE_TYPE_INLONGMSG(2, "InLongMsg", "The InLongMsg encode format"),
    MSG_ENCODE_TYPE_UNKNOWN(99, "Unknown", "Unknown encode format");

    DataProxyMsgEncType(int id, String name, String desc) {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getStrId() {
        return String.valueOf(id);
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public static DataProxyMsgEncType valueOf(int value) {
        for (DataProxyMsgEncType msgEncType : DataProxyMsgEncType.values()) {
            if (msgEncType.getId() == value) {
                return msgEncType;
            }
        }
        return MSG_ENCODE_TYPE_UNKNOWN;
    }

    private final int id;
    private final String name;
    private final String desc;
}
