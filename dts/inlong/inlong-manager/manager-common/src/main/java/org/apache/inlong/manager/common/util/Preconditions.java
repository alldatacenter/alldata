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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.inlong.manager.common.enums.ErrorCodeEnum;
import org.apache.inlong.manager.common.exceptions.BusinessException;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Parameter verification tools
 */
public class Preconditions {

    public static void expectNotNull(Object obj, String errMsg) {
        expectTrue(obj != null, errMsg);
    }

    public static void expectNotNull(Object obj, Supplier<String> errMsg) {
        expectTrue(obj != null, errMsg);
    }

    public static void expectNull(Object obj, String errMsg) {
        expectTrue(obj == null, errMsg);
    }

    public static void expectNull(Object obj, Supplier<String> errMsg) {
        expectTrue(obj == null, errMsg);
    }

    public static void expectEmpty(String str, String errMsg) {
        expectTrue(str == null || str.isEmpty(), errMsg);
    }

    public static void expectEmpty(String str, Supplier<String> errMsg) {
        expectTrue(str == null || str.isEmpty(), errMsg);
    }

    public static void expectEmpty(Collection<?> str, String errMsg) {
        expectTrue(str == null || str.isEmpty(), errMsg);
    }

    public static void expectEmpty(Collection<?> collection, Supplier<String> errMsg) {
        expectTrue(collection == null || collection.isEmpty(), errMsg);
    }

    public static void expectEmpty(Map<?, ?> map, String errMsg) {
        expectTrue(map == null || map.isEmpty(), errMsg);
    }

    public static void expectEmpty(Map<?, ?> map, Supplier<String> errMsg) {
        expectTrue(map == null || map.isEmpty(), errMsg);
    }

    public static void expectNotEmpty(String str, String errMsg) {
        expectTrue(str != null && !str.isEmpty(), errMsg);
    }

    public static void expectNotEmpty(String str, Supplier<String> errMsg) {
        expectTrue(str != null && !str.isEmpty(), errMsg);
    }

    public static void expectNotEmpty(Collection<?> collection, String errMsg) {
        expectTrue(collection != null && !collection.isEmpty(), errMsg);
    }

    public static void expectNotEmpty(Collection<?> collection, Supplier<String> errMsg) {
        expectTrue(collection != null && !collection.isEmpty(), errMsg);
    }

    public static void expectNotEmpty(String[] collection, String errMsg) {
        expectTrue(collection != null && collection.length != 0, errMsg);
    }

    public static void expectNotEmpty(String[] collection, Supplier<String> errMsg) {
        expectTrue(collection != null && collection.length != 0, errMsg);
    }

    public static void expectNotEmpty(Map<?, ?> map, String errMsg) {
        expectTrue(map != null && !map.isEmpty(), errMsg);
    }

    public static void expectNotEmpty(Map<?, ?> map, Supplier<String> errMsg) {
        expectTrue(map != null && !map.isEmpty(), errMsg);
    }

    public static void expectNoNullElements(Object[] array, String errMsg) {
        if (array != null) {
            for (Object o : array) {
                if (o == null) {
                    throw new IllegalArgumentException(errMsg);
                }
            }
        }
    }

    public static void expectFalse(boolean condition, String errMsg) {
        expectTrue(!condition, errMsg);
    }

    public static void expectFalse(boolean condition, Supplier<String> errMsg) {
        expectTrue(!condition, errMsg);
    }

    public static void expectTrue(boolean condition, Supplier<String> errMsg) {
        if (!condition) {
            throw new IllegalArgumentException(errMsg.get());
        }
    }

    public static void expectTrue(boolean condition, String errMsg) {
        if (!condition) {
            throw new IllegalArgumentException(errMsg);
        }
    }
    public static void expectNotBlank(String obj, String errMsg) {
        if (StringUtils.isBlank(obj)) {
            throw new IllegalArgumentException(errMsg);
        }
    }

    public static void expectNotBlank(String obj, ErrorCodeEnum errorCodeEnum) {
        if (StringUtils.isBlank(obj)) {
            throw new BusinessException(errorCodeEnum);
        }
    }

    public static void expectNotBlank(String obj, ErrorCodeEnum errorCodeEnum, String errMsg) {
        if (StringUtils.isBlank(obj)) {
            throw new BusinessException(errorCodeEnum, errMsg);
        }
    }

    public static void expectEmpty(List<String> obj, ErrorCodeEnum errorCodeEnum, String errMsg) {
        if (CollectionUtils.isNotEmpty(obj)) {
            throw new BusinessException(errorCodeEnum, errMsg);
        }
    }

    public static void expectNotNull(Object obj, ErrorCodeEnum errorCodeEnum) {
        if (obj == null) {
            throw new BusinessException(errorCodeEnum);
        }
    }

    public static void expectNotNull(Object obj, ErrorCodeEnum errorCodeEnum, String errMsg) {
        if (obj == null) {
            throw new BusinessException(errorCodeEnum, errMsg);
        }
    }

    public static void expectEquals(Object a, Object b, ErrorCodeEnum errorCodeEnum) {
        if (!Objects.equals(a, b)) {
            throw new BusinessException(errorCodeEnum);
        }
    }

    public static void expectEquals(Object a, Object b, ErrorCodeEnum errorCodeEnum, String errMsg) {
        if (!Objects.equals(a, b)) {
            throw new BusinessException(errorCodeEnum, errMsg);
        }
    }
    /**
     * Whether a target string is in a string separated by the separator.
     *
     * @param target target string, such as "foo"
     * @param separatedStr separated string, such as "boo,and,foo"
     * @param separator separator of separatedStr, such as ","
     * @return true if target in separatedStr
     */
    public static boolean inSeparatedString(String target, String separatedStr, String separator) {
        if (StringUtils.isBlank(target) || StringUtils.isBlank(separatedStr)) {
            return false;
        }
        Set<String> set = new HashSet<>(Arrays.asList(separatedStr.split(separator)));
        return set.contains(target);
    }
}
