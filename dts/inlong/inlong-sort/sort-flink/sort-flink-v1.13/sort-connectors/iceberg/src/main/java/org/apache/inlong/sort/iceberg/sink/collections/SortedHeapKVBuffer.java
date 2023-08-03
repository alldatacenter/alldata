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

package org.apache.inlong.sort.iceberg.sink.collections;

import org.apache.flink.api.java.tuple.Tuple2;

import java.io.Serializable;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.stream.Stream;

public class SortedHeapKVBuffer<T, R> implements KVBuffer<T, R>, Serializable {

    private static final long serialVersionUID = 1L;
    private final NavigableMap<T, R> map;
    private final KVBuffer.Convertor<T> convertor;

    public SortedHeapKVBuffer(KVBuffer.Convertor<T> convertor) {
        this.convertor = convertor;
        this.map = new TreeMap<>(convertor);
    }

    @Override
    public R put(T key, R value) {
        return map.put(key, value);
    }

    @Override
    public R remove(T key) {
        return map.remove(key);
    }

    @Override
    public R get(T key) {
        return map.get(key);
    }

    @Override
    public Stream<Tuple2<T, R>> scan(byte[] keyPrefix) {
        return map.subMap(convertor.lower(keyPrefix), true, convertor.upper(keyPrefix), true)
                .entrySet()
                .stream()
                .map(entry -> new Tuple2<>(entry.getKey(), entry.getValue()));
    }

    @Override
    public void clear() {
        map.clear();
    }
}
