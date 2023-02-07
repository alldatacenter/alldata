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

package org.apache.inlong.dataproxy.utils;

public enum InLongMsgVer {

    INLONG_V0(0, "V0", "The inlong-msg V0 format"),
    INLONG_V1(1, "V1", "The inlong-msg V1 format");

    InLongMsgVer(int id, String name, String desc) {
        this.id = id;
        this.name = name;
        this.desc = desc;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public static InLongMsgVer valueOf(int value) {
        for (InLongMsgVer inLongMsgVer : InLongMsgVer.values()) {
            if (inLongMsgVer.getId() == value) {
                return inLongMsgVer;
            }
        }
        return INLONG_V0;
    }

    private final int id;
    private final String name;
    private final String desc;
}
