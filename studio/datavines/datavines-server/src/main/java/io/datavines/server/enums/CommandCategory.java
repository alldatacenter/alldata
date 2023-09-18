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

public enum CommandCategory {

    /**
     * 0-data quality task
     * 1-catalog task
     */
    DATA_QUALITY(0, "data quality task"),
    CATALOG(1, "catalog task")
    ;
    CommandCategory(int code, String description){
        this.code = code;
        this.description = description;
    }

    @EnumValue
    private final int code;

    private final String description;

    private static final Map<Integer, CommandCategory> COMMAND_CATEGORY_MAP = new HashMap<>();

    static {
        for (CommandCategory commandCategory : CommandCategory.values()) {
            COMMAND_CATEGORY_MAP.put(commandCategory.code,commandCategory);
        }
    }

    public static CommandCategory of(Integer status) {
        if (COMMAND_CATEGORY_MAP.containsKey(status)) {
            return COMMAND_CATEGORY_MAP.get(status);
        }
        throw new IllegalArgumentException("invalid command category : " + status);
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
