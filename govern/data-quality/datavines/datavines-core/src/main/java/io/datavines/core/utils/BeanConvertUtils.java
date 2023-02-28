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
package io.datavines.core.utils;

import org.springframework.beans.BeanUtils;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class BeanConvertUtils {

    public static <T>  T convertBean(Object source, Supplier<T> supplier){
        T target = supplier.get();
        BeanUtils.copyProperties(source, target);
        return target;
    }

    /**
     * convert bean collection to another type bean collection
     * @param source
     * @param supplier
     * @param <T>
     * @return
     */
    public static <T> List<T> convertBeanCollection(Collection source, Supplier<T> supplier){
        if (CollectionUtils.isEmpty(source)){
            return new ArrayList<T>();
        }
        Stream<T> stream = source.stream().map(x -> {
            T target = supplier.get();
            BeanUtils.copyProperties(source, target);
            return target;
        });
        List<T> collect = stream.collect(Collectors.toList());
        return collect;
    }
}
