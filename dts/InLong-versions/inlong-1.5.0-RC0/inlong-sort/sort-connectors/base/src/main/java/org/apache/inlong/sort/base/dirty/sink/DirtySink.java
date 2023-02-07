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

package org.apache.inlong.sort.base.dirty.sink;

import org.apache.flink.configuration.Configuration;
import org.apache.inlong.sort.base.dirty.DirtyData;

import java.io.Serializable;

/**
 * The dirty sink base inteface
 *
 * @param <T>
 */
public interface DirtySink<T> extends Serializable {

    /**
     * Open for dirty sink
     *
     * @param configuration The configuration that is used for dirty sink
     * @throws Exception The exception may be thrown when executing
     */
    default void open(Configuration configuration) throws Exception {

    }

    /**
     * Invoke that is used to sink dirty data
     *
     * @param dirtyData The dirty data that will be written
     * @throws Exception The exception may be thrown when executing
     */
    void invoke(DirtyData<T> dirtyData) throws Exception;

    /**
     * Close for dirty sink
     *
     * @throws Exception The exception may be thrown when executing
     */
    default void close() throws Exception {

    }

}
