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

public enum TransformType {

    /**
     * 0 invalidate items
     * 1 actual value
     * 2 expected value from default source
     * 3 expected value from src source
     * 4 expected value from target source
     **/
    INVALIDATE_ITEMS(0, "invalidate_items"),
    ACTUAL_VALUE(1, "actual_value"),
    EXPECTED_VALUE_FROM_METADATA_SOURCE(2, "expected_value_from_metadata_source"),
    EXPECTED_VALUE_FROM_SOURCE(3, "expected_value_from_source"),
    EXPECTED_VALUE_FROM_TARGET_SOURCE(4, "expected_value_from_target_source");

    TransformType(int code, String description){
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

    private static final HashMap<Integer, TransformType> SINK_TYPE_MAP = new HashMap<>();

    static {
        for (TransformType sinkType: TransformType.values()){
            SINK_TYPE_MAP.put(sinkType.code,sinkType);
        }
    }

    public static TransformType of(int transform){
        if(SINK_TYPE_MAP.containsKey(transform)){
            return SINK_TYPE_MAP.get(transform);
        }
        throw new IllegalArgumentException("invalid transform type : " + transform);
    }

    public static TransformType of(String transform){

        for (TransformType transformType: TransformType.values()){
            if(transformType.getDescription().equals(transform)){
                return transformType;
            }
        }
        throw new IllegalArgumentException("invalid transform type : " + transform);
    }
}
