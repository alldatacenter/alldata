/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.datavines.server.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.HashMap;
import java.util.Map;

public enum ScheduleJobType {
    /**
     *
     */
    DATA_QUALITY(0, "data_quality"),
    CATALOG(1, "catalog");

    ScheduleJobType(int code, String description){
        this.code = code;
        this.description = description;
    }

    private int code;

    @EnumValue
    private String description;

    private static final Map<String, ScheduleJobType> TYPE_MAP = new HashMap<>();

    static {
        for (ScheduleJobType jobScheduleType : ScheduleJobType.values()) {
            TYPE_MAP.put(jobScheduleType.description,jobScheduleType);
        }
    }

    public static ScheduleJobType of(String type) {
        if (TYPE_MAP.containsKey(type)) {
            return TYPE_MAP.get(type);
        }
        throw new IllegalArgumentException("invalid schedule job type : " + type);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
