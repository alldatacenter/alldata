/**
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
package org.apache.atlas.utils;

import java.util.Arrays;
import java.util.Collection;

/**
 * Utilities for checking parameters.
 */
public final class ParamChecker {

    private ParamChecker() {
    }

    /**
     * Check that a value is not null. If null throws an IllegalArgumentException.
     *
     * @param obj value.
     * @param name parameter name for the exception message.
     * @return the given value.
     */
    public static <T> T notNull(T obj, String name) {
        if (obj == null) {
            throw new IllegalArgumentException(name + " cannot be null");
        }
        return obj;
    }

    /**
     * Check that a list is not null and that none of its elements is null. If null or if the list has emtpy elements
     * throws an IllegalArgumentException.
     *  @param list the list of T.
     * @param name parameter name for the exception message.
     */
    public static <T> Collection<T> notNullElements(Collection<T> list, String name) {
        notEmpty(list, name);
        for (T ele : list) {
            notNull(ele, String.format("Collection %s element %s", name, ele));
        }
        return list;
    }

    /**
     * Check that a list is not null and that none of its elements is null. If null or if the list has emtpy elements
     * throws an IllegalArgumentException.
     *  @param array the array of T.
     * @param name parameter name for the exception message.
     */
    public static <T> T[] notNullElements(T[] array, String name) {
        notEmpty(Arrays.asList(array), name);
        for (T ele : array) {
            notNull(ele, String.format("Collection %s element %s", name, ele));
        }
        return array;
    }

    /**
     * Check that a list is not null and not empty.
     *  @param list the list of T.
     * @param name parameter name for the exception message.
     */
    public static <T> Collection<T> notEmpty(Collection<T> list, String name) {
        notNull(list, name);
        if (list.isEmpty()) {
            throw new IllegalArgumentException(String.format("Collection %s is empty", name));
        }
        return list;
    }

    /**
     * Check that a string is not null and not empty. If null or emtpy throws an IllegalArgumentException.
     *
     * @param value value.
     * @param name parameter name for the exception message.
     * @return the given value.
     */
    public static String notEmpty(String value, String name) {
        return notEmpty(value, name, null);
    }

    /**
     * Check that a string is not empty if its not null.
     *
     * @param value value.
     * @param name parameter name for the exception message.
     * @return the given value.
     */
    public static String notEmptyIfNotNull(String value, String name) {
        return notEmptyIfNotNull(value, name, null);
    }

    /**
     * Check that a string is not empty if its not null.
     *
     * @param value value.
     * @param name parameter name for the exception message.
     * @return the given value.
     */
    public static String notEmptyIfNotNull(String value, String name, String info) {
        if (value == null) {
            return value;
        }

        if (value.trim().length() == 0) {
            throw new IllegalArgumentException(name + " cannot be empty" + (info == null ? "" : ", " + info));
        }
        return value.trim();
    }

    /**
     * Check that a string is not null and not empty. If null or emtpy throws an IllegalArgumentException.
     *
     * @param value value.
     * @param name parameter name for the exception message.
     * @param info additional information to be printed with the exception message
     * @return the given value.
     */
    public static String notEmpty(String value, String name, String info) {
        if (value == null) {
            throw new IllegalArgumentException(name + " cannot be null" + (info == null ? "" : ", " + info));
        }
        return notEmptyIfNotNull(value, name, info);
    }

    /**
     * Checks that the given value is <= max value.
     * @param value
     * @param maxValue
     * @param name
     */
    public static void lessThan(long value, long maxValue, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " should be > 0, current value " + value);
        }
        if (value > maxValue) {
            throw new IllegalArgumentException(name + " should be <= " + maxValue + ", current value " + value);
        }
    }

    public static void greaterThan(long value, long minValue, String name) {
        if (value <= minValue) {
            throw new IllegalArgumentException(name + " should be > " + minValue + ", current value " + value);
        }
    }
}
