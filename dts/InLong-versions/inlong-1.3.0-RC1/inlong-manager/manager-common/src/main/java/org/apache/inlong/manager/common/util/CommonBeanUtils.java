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

import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;

/**
 * Copy the tool class of the Bean property in the List
 */
public class CommonBeanUtils extends BeanUtils {

    /**
     * Usage scenario: Loop replication for each Java entity in the List
     *
     * @param sources Source entity list
     * @param target  target entity list
     * @param <S>     The type of the source entity list
     * @param <T>     The type of the target entity list
     * @return target entity list
     */
    public static <S, T> List<T> copyListProperties(List<S> sources, Supplier<T> target) {
        if (sources == null || sources.isEmpty()) {
            return new ArrayList<>(0);
        }
        List<T> list = new ArrayList<>(sources.size());
        for (S source : sources) {
            T t = target.get();
            copyProperties(source, t);
            list.add(t);
        }
        return list;
    }

    /**
     * Copy the content of the source data to the object of the target type, and return the result
     *
     * @param source source data content
     * @param target target type
     * @param <S>    source type
     * @param <T>    target type
     * @return the target type object after copying
     */
    public static <S, T> T copyProperties(S source, Supplier<T> target) {
        T result = target.get();
        if (source == null) {
            return result;
        }
        copyProperties(source, result);
        return result;
    }

    /**
     * Copy the content of the source instance to the target instance
     *
     * @param source     source data content
     * @param target     target data
     * @param ignoreNull Whether to ignore null values
     * @param <S>        source type
     * @param <T>        target type
     * @apiNote If ignoreNull = false, non-null attributes in the target instance may be overwritten
     */
    public static <S, T> T copyProperties(S source, T target, boolean ignoreNull) {
        if (source == null) {
            return target;
        }
        if (ignoreNull) {
            copyProperties(source, target, getNullPropertyNames(source));
        } else {
            copyProperties(source, target);
        }

        return target;
    }

    /**
     * Get an array of null field names for a given object
     *
     * @param source target object
     * @return an array of field names whose value is null
     */
    public static String[] getNullPropertyNames(Object source) {
        final BeanWrapper src = new BeanWrapperImpl(source);
        java.beans.PropertyDescriptor[] pds = src.getPropertyDescriptors();

        Set<String> emptyNames = new HashSet<>();
        for (java.beans.PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        String[] result = new String[emptyNames.size()];
        return emptyNames.toArray(result);
    }

}
