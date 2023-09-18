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

public enum SourceType {

    /**
     * 0 normal
     * 1 invalidate items
     * 2 actual value
     **/
    SOURCE(0, "source"),
    TARGET(1, "target"),
    METADATA(2, "metadata");

    SourceType(int code, String description){
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

    private static final HashMap<Integer, SourceType> SINK_TYPE_MAP = new HashMap<>();

    static {
        for (SourceType sinkType: SourceType.values()){
            SINK_TYPE_MAP.put(sinkType.code,sinkType);
        }
    }

    public static SourceType of(int source){
        if(SINK_TYPE_MAP.containsKey(source)){
            return SINK_TYPE_MAP.get(source);
        }
        throw new IllegalArgumentException("invalid source type : " + source);
    }

    public static SourceType of(String source){

        for (SourceType sinkType: SourceType.values()){
            if(sinkType.getDescription().equals(source)){
                return sinkType;
            }
        }
        throw new IllegalArgumentException("invalid source type : " + source);
    }
}
