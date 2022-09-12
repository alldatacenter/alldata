/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.inlong.manager.common.util;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Parameter verification tools
 */
public class Preconditions {

    public static void checkNotNull(Object obj, String errMsg) {
        checkTrue(obj != null, errMsg);
    }

    public static void checkNotNull(Object obj, Supplier<String> errMsg) {
        checkTrue(obj != null, errMsg);
    }

    public static void checkNull(Object obj, String errMsg) {
        checkTrue(obj == null, errMsg);
    }

    public static void checkNull(Object obj, Supplier<String> errMsg) {
        checkTrue(obj == null, errMsg);
    }

    public static void checkEmpty(String str, String errMsg) {
        checkTrue(str == null || str.isEmpty(), errMsg);
    }

    public static void checkEmpty(String str, Supplier<String> errMsg) {
        checkTrue(str == null || str.isEmpty(), errMsg);
    }

    public static void checkEmpty(Collection<?> str, String errMsg) {
        checkTrue(str == null || str.isEmpty(), errMsg);
    }

    public static void checkEmpty(Collection<?> collection, Supplier<String> errMsg) {
        checkTrue(collection == null || collection.isEmpty(), errMsg);
    }

    public static void checkEmpty(Map<?, ?> map, String errMsg) {
        checkTrue(map == null || map.isEmpty(), errMsg);
    }

    public static void checkEmpty(Map<?, ?> map, Supplier<String> errMsg) {
        checkTrue(map == null || map.isEmpty(), errMsg);
    }

    public static void checkNotEmpty(String str, String errMsg) {
        checkTrue(str != null && !str.isEmpty(), errMsg);
    }

    public static void checkNotEmpty(String str, Supplier<String> errMsg) {
        checkTrue(str != null && !str.isEmpty(), errMsg);
    }

    public static void checkNotEmpty(Collection<?> collection, String errMsg) {
        checkTrue(collection != null && !collection.isEmpty(), errMsg);
    }

    public static void checkNotEmpty(Collection<?> collection, Supplier<String> errMsg) {
        checkTrue(collection != null && !collection.isEmpty(), errMsg);
    }

    public static void checkNotEmpty(Map<?, ?> map, String errMsg) {
        checkTrue(map != null && !map.isEmpty(), errMsg);
    }

    public static void checkNotEmpty(Map<?, ?> map, Supplier<String> errMsg) {
        checkTrue(map != null && !map.isEmpty(), errMsg);
    }

    public static void checkFalse(boolean condition, String errMsg) {
        checkTrue(!condition, errMsg);
    }

    public static void checkFalse(boolean condition, Supplier<String> errMsg) {
        checkTrue(!condition, errMsg);
    }

    public static void checkTrue(boolean condition, Supplier<String> errMsg) {
        if (!condition) {
            throw new IllegalArgumentException(errMsg.get());
        }
    }

    public static void checkTrue(boolean condition, String errMsg) {
        if (!condition) {
            throw new IllegalArgumentException(errMsg);
        }
    }

}
