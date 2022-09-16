/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.sdk.dataproxy.codec;

import java.util.HashMap;
import java.util.Map;

public enum ErrorCode {

    ATTR_ERROR(1),

    DT_ERROR(2),

    COMPRESS_ERROR(3),

    OTHER_ERROR(4),

    LONG_LENGTH_ERROR(5);
    private final int value;
    private static final Map<Integer, ErrorCode> map = new HashMap<>();

    static {
        for (ErrorCode errorCode : ErrorCode.values()) {
            map.put(errorCode.value, errorCode);
        }
    }

    ErrorCode(int value) {
        this.value = value;
    }

    public static ErrorCode valueOf(int value) {
        return map.get(value);
    }

}
