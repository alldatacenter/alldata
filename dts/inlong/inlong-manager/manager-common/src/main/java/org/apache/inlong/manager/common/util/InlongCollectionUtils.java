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

import com.google.common.collect.ImmutableMap;
import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.collections.CollectionUtils;

/**
 * Utils of inlong collection.
 */
public class InlongCollectionUtils {

    /**
     * Transform input collection to ImmutableMap, if input collection is null, then return empty map
     *
     * @param originCollection       origin collection
     * @param keyTransformFunction   key transform function
     * @param valueTransformFunction value transform function
     * @param <K>                    key type
     * @param <V>                    value type
     * @param <T>                    origin type
     * @return {@link ImmutableMap}
     */
    public static <K, V, T> Map<K, V> transformToImmutableMap(Collection<T> originCollection,
            Function<T, K> keyTransformFunction,
            Function<T, V> valueTransformFunction) {
        if (CollectionUtils.isEmpty(originCollection)) {
            return ImmutableMap.of();
        }
        Preconditions.expectNotNull(keyTransformFunction, "KeyTransformFunction cannot be null");
        Preconditions.expectNotNull(valueTransformFunction, "ValueTransformFunction cannot be null");

        ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
        originCollection.forEach(originObject -> builder.put(keyTransformFunction.apply(originObject),
                valueTransformFunction.apply(originObject)));
        return builder.build();
    }
}
