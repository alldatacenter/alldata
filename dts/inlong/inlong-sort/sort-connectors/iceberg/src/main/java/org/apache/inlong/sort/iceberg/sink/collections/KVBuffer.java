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

import java.util.Comparator;
import java.util.stream.Stream;

/**
 * A buffer that can store KV data is more concerned with data storage and query, so this buffer may be very large.
 * And it can range data according to the prefix of the key, and all key-value pairs that meet the prefix of the key
 * will be returned.
 *
 * For the implementation of ordered storage, {@link Convertor} returns the upper and lower bound keys for
 * a specific key prefix
 *
 * @param <K> unique key data
 * @param <V> value
 */
public interface KVBuffer<K, V> {

    /**
     * Associates the specified value with the specified key in this buffer
     * (optional operation).  If the buffer previously contained a mapping for
     * the key, the old value is replaced by the specified value.
     *
     * @param key key with which the specified value is to be associated
     * @param value value to be associated with the specified key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    V put(K key, V value);

    /**
     * Removes the mapping for a key from this buffer if it is present
     * (optional operation).   More formally, if this buffer contains a mapping
     * from key <tt>k</tt> to value <tt>v</tt> such that
     * <code>(key==null ?  k==null : key.equals(k))</code>, that mapping
     * is removed.  (The buffer can contain at most one such mapping.)
     *
     * @param key
     * @return the previous value associated with <tt>key</tt>, or
     *         <tt>null</tt> if there was no mapping for <tt>key</tt>.
     */
    V remove(K key);

    /**
     * Returns the value to which the specified key is mapped,
     * or {@code null} if this buffer contains no mapping for the key.
     *
     * @param key
     * @return the value to which the specified key is mapped
     */
    V get(K key);

    /**
     * Perform a range search according to the prefix of the key, and the KV data of all keys matching the
     * prefix will be returned
     *
     * @param keyPrefix key prefix binary data
     * @return KV pairs that match the key prefix
     */
    Stream<Tuple2<K, V>> scan(byte[] keyPrefix);

    /**
     * Removes all of the KV pair from this buffer (optional operation).
     * The Buffer will be empty after this call returns.
     */
    void clear();

    /**
     * Convertor to convert a key prefix to key range.
     * @param <K>
     */
    interface Convertor<K> extends Comparator<K> {

        K upper(byte[] keyPrefix);

        K lower(byte[] keyPrefix);
    }
}
