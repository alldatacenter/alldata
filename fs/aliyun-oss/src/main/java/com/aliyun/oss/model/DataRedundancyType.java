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
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.aliyun.oss.model;

/**
 * Data redundancy type
 */
public enum DataRedundancyType {
    /**
     * LRS
     */
    LRS("LRS"),

    /**
     * ZRS
     */
    ZRS("ZRS");

    private final String dataRedundancyTypeString;

    private DataRedundancyType(String dataRedundancyTypeString) {
        this.dataRedundancyTypeString = dataRedundancyTypeString;
    }

    @Override
    public String toString() {
        return dataRedundancyTypeString;
    }

    /**
     * Returns the DataRedundancyType enum corresponding to the given string
     *
     * @param dataRedundancyTypeString
     *            data redundancy type.
     *
     * @return  The {@link DataRedundancyType} instance.
     * 
     * @throws IllegalArgumentException if the specified dataRedundancyTypeString is not
     * supported.
     */
    public static DataRedundancyType parse(String dataRedundancyTypeString) {
        for (DataRedundancyType e: DataRedundancyType.values()) {
            if (e.toString().equals(dataRedundancyTypeString))
                return e;
        }
        throw new IllegalArgumentException("Unsupported data redundancy type: " + dataRedundancyTypeString);
    }
}