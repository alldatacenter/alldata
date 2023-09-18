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
package io.datavines.common.config.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;

import java.util.HashMap;

public enum SinkType {

    /**
     * 0 error data.
     * 1 validate result
     * 2 actual value
     **/
    ERROR_DATA(0, "error_data"),
    VALIDATE_RESULT(1, "validate_result"),
    ACTUAL_VALUE(2, "actual_value"),
    PROFILE_VALUE(3, "profile_value");

    SinkType(int code, String description){
        this.code = code;
        this.description = description;
    }

    @EnumValue
    final int code;

    final String description;

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    private static final HashMap<Integer, SinkType> SINK_TYPE_MAP = new HashMap<>();

    static {
        for (SinkType sinkType: SinkType.values()){
            SINK_TYPE_MAP.put(sinkType.code,sinkType);
        }
    }

    public static SinkType of(int sink){
        if(SINK_TYPE_MAP.containsKey(sink)){
            return SINK_TYPE_MAP.get(sink);
        }
        throw new IllegalArgumentException("invalid sink type : " + sink);
    }

    public static SinkType of(String sink){

        for (SinkType sinkType: SinkType.values()){
            if(sinkType.getDescription().equals(sink)){
                return sinkType;
            }
        }
        throw new IllegalArgumentException("invalid sink type : " + sink);
    }
}
