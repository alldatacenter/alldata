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

package com.aliyun.oss.common.utils;

import static com.aliyun.oss.internal.OSSUtils.COMMON_RESOURCE_MANAGER;

import java.util.List;

/**
 * Utils for common coding.
 */
public class CodingUtils {

    public static void assertParameterNotNull(Object param, String paramName) {
        if (param == null) {
            throw new NullPointerException(COMMON_RESOURCE_MANAGER.getFormattedString("ParameterIsNull", paramName));
        }
    }

    public static void assertParameterInRange(long param, long lower, long upper) {
        if (!checkParamRange(param, lower, true, upper, true)) {
            throw new IllegalArgumentException(String.format("%d not in valid range [%d, %d]", param, lower, upper));
        }
    }

    public static void assertStringNotNullOrEmpty(String param, String paramName) {
        assertParameterNotNull(param, paramName);
        if (param.trim().length() == 0) {
            throw new IllegalArgumentException(
                    COMMON_RESOURCE_MANAGER.getFormattedString("ParameterStringIsEmpty", paramName));
        }
    }

    public static void assertListNotNullOrEmpty(List<?> param, String paramName) {
        assertParameterNotNull(param, paramName);
        if (param.size() == 0) {
            throw new IllegalArgumentException(
                    COMMON_RESOURCE_MANAGER.getFormattedString("ParameterListIsEmpty", paramName));
        }
    }

    public static boolean isNullOrEmpty(String value) {
        return value == null || value.length() == 0;
    }

    public static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static boolean checkParamRange(long param, long from, boolean leftInclusive, long to,
            boolean rightInclusive) {

        if (leftInclusive && rightInclusive) { // [from, to]
            if (from <= param && param <= to) {
                return true;
            } else {
                return false;
            }
        } else if (leftInclusive && !rightInclusive) { // [from, to)
            if (from <= param && param < to) {
                return true;
            } else {
                return false;
            }
        } else if (!leftInclusive && !rightInclusive) { // (from, to)
            if (from < param && param < to) {
                return true;
            } else {
                return false;
            }
        } else { // (from, to]
            if (from < param && param <= to) {
                return true;
            } else {
                return false;
            }
        }
    }
}
