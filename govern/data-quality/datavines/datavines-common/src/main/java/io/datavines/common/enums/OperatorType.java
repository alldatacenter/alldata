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
package io.datavines.common.enums;

import com.fasterxml.jackson.annotation.JsonValue;

import java.util.HashMap;
import java.util.Map;

/**
 * operator type
 */
public enum OperatorType {
    /**
     * 0-equal
     * 1-little than
     * 2-little and equal
     * 3-great than
     * 4-great and equal
     * 5-not equal
     */
    EQ(0,"eq","="),
    LT(1,"lt","<"),
    LTE(2,"lte","<="),
    GT(3,"gt",">"),
    GTE(4,"gte",">="),
    NE(5,"neq","!=");

    OperatorType(int code, String description,String symbol) {
        this.code = code;
        this.description = description;
        this.symbol = symbol;
    }

    private final int code;
    private final String description;
    private final String symbol;

    public int getCode() {
        return code;
    }

    @JsonValue
    public String getDescription() {
        return description;
    }

    public String getSymbol() {
        return symbol;
    }

    private static final Map<Integer, OperatorType> VALUES_MAP = new HashMap<>();

    static {
        for (OperatorType type : OperatorType.values()) {
            VALUES_MAP.put(type.code,type);
        }
    }

    public static OperatorType of(Integer status) {
        if (VALUES_MAP.containsKey(status)) {
            return VALUES_MAP.get(status);
        }
        throw new IllegalArgumentException("invalid code : " + status);
    }

    public static OperatorType of(String operator) {
        for (OperatorType type : OperatorType.values()) {
            if (type.getDescription().equals(operator)){
                return type;
            }
        }
        throw new IllegalArgumentException("invalid code : " + operator);
    }
}