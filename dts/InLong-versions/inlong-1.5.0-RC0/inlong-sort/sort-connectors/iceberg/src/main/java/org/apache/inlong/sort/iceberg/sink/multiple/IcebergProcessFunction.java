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

package org.apache.inlong.sort.iceberg.sink.multiple;

import org.apache.flink.api.common.functions.AbstractRichFunction;
import org.apache.flink.api.common.functions.RuntimeContext;
import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.function.ThrowingConsumer;

public abstract class IcebergProcessFunction<IN, OUT> extends AbstractRichFunction {

    private static final long serialVersionUID = 1L;

    protected transient Collector<OUT> collector;

    protected transient Context context;

    public abstract void processElement(IN value) throws Exception;

    public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {

    }

    public void endInput() throws Exception {

    }

    public void dispose() throws Exception {

    }

    public void setup(RuntimeContext t, Collector<OUT> collector, Context context) {
        setRuntimeContext(t);
        setCollector(collector);
        setContext(context);
    }

    public void setCollector(Collector<OUT> collector) {
        this.collector = collector;
    }

    public void setContext(Context context) {
        this.context = context;
    }

    public abstract static class Context {

        /**
         * Timestamp of the element currently being processed or timestamp of a firing timer.
         *
         * <p>This might be {@code null}, for example if the time characteristic of your program is
         * set to {@link org.apache.flink.streaming.api.TimeCharacteristic#ProcessingTime}.
         */
        public abstract Long timestamp();

        /** A {@link TimerService} for querying time and registering timers. */
        public abstract TimerService timerService();

        /**
         * Emits a record to the side output identified by the {@link OutputTag}.
         *
         * @param outputTag the {@code OutputTag} that identifies the side output to emit to.
         * @param value The record to emit.
         */
        public abstract <X> void output(OutputTag<X> outputTag, X value);
    }

    public static class CallbackCollector<T> implements Collector<T> {

        final ThrowingConsumer<T, Exception> callback;

        CallbackCollector(ThrowingConsumer<T, Exception> callback) {
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
}