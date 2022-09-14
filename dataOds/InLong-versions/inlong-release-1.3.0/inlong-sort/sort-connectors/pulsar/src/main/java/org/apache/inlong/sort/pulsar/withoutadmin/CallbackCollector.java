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

package org.apache.inlong.sort.pulsar.withoutadmin;

import org.apache.flink.util.Collector;
import org.apache.flink.util.function.ThrowingConsumer;

/**
 * A collector supporting callback.
 */
public class CallbackCollector<T> implements Collector<T> {

    private final ThrowingConsumer<T, Exception> callback;

    public CallbackCollector(ThrowingConsumer<T, Exception> callback) {
        this.callback = callback;
    }

    @Override
    public void collect(T t) {
        try {
            callback.accept(t);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void close() {

    }
}
