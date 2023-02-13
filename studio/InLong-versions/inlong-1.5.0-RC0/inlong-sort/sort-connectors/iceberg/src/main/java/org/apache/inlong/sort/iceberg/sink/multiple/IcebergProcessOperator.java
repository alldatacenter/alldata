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

import org.apache.flink.streaming.api.TimerService;
import org.apache.flink.streaming.api.operators.AbstractUdfStreamOperator;
import org.apache.flink.streaming.api.operators.BoundedOneInput;
import org.apache.flink.streaming.api.operators.OneInputStreamOperator;
import org.apache.flink.streaming.api.operators.TimestampedCollector;
import org.apache.flink.streaming.api.watermark.Watermark;
import org.apache.flink.streaming.runtime.streamrecord.StreamRecord;
import org.apache.flink.streaming.runtime.tasks.ProcessingTimeService;
import org.apache.flink.util.OutputTag;
import org.apache.inlong.sort.iceberg.sink.multiple.IcebergProcessFunction.Context;

public class IcebergProcessOperator<IN, OUT>
        extends
            AbstractUdfStreamOperator<OUT, IcebergProcessFunction<IN, OUT>>
        implements
            OneInputStreamOperator<IN, OUT>,
            BoundedOneInput {

    private static final long serialVersionUID = -1837485654246776219L;

    private long currentWatermark = Long.MIN_VALUE;

    private transient TimestampedCollector<OUT> collector;

    private transient ContextImpl context;

    public IcebergProcessOperator(IcebergProcessFunction<IN, OUT> userFunction) {
        super(userFunction);
    }

    @Override
    public void open() throws Exception {
        super.open();
        collector = new TimestampedCollector<>(output);
        context = new ContextImpl(getProcessingTimeService());
        userFunction.setCollector(collector);
        userFunction.setContext(new ContextImpl(getProcessingTimeService()));
    }

    @Override
    public void processElement(StreamRecord<IN> streamRecord) throws Exception {
        collector.setTimestamp(streamRecord);
        context.element = streamRecord;
        userFunction.processElement(streamRecord.getValue());
        context.element = null;
    }

    @Override
    public void processWatermark(Watermark mark) throws Exception {
        super.processWatermark(mark);
        this.currentWatermark = mark.getTimestamp();
    }

    @Override
    public void prepareSnapshotPreBarrier(long checkpointId) throws Exception {
        super.prepareSnapshotPreBarrier(checkpointId);
        userFunction.prepareSnapshotPreBarrier(checkpointId);
    }

    @Override
    public void endInput() throws Exception {
        userFunction.endInput();
    }

    @Override
    public void dispose() throws Exception {
        super.dispose();
        userFunction.dispose();
    }

    private class ContextImpl extends Context implements TimerService {

        private StreamRecord<IN> element;

        private final ProcessingTimeService processingTimeService;

        ContextImpl(ProcessingTimeService processingTimeService) {
            this.processingTimeService = processingTimeService;
        }

        @Override
        public Long timestamp() {
            return element == null ? null : element.getTimestamp();
        }

        @Override
        public TimerService timerService() {
            return this;
        }

        @Override
        public <X> void output(OutputTag<X> outputTag, X value) {
            if (outputTag == null) {
                throw new IllegalArgumentException("OutputTag must not be null.");
            }
            output.collect(outputTag, new StreamRecord<>(value, element.getTimestamp()));
        }

        @Override
        public long currentProcessingTime() {
            return processingTimeService.getCurrentProcessingTime();
        }

        @Override
        public long currentWatermark() {
            return currentWatermark;
        }

        @Override
        public void registerProcessingTimeTimer(long time) {
            throw new UnsupportedOperationException(UNSUPPORTED_REGISTER_TIMER_MSG);
        }

        @Override
        public void registerEventTimeTimer(long time) {
            throw new UnsupportedOperationException(UNSUPPORTED_REGISTER_TIMER_MSG);
        }

        @Override
        public void deleteProcessingTimeTimer(long time) {
            throw new UnsupportedOperationException(UNSUPPORTED_DELETE_TIMER_MSG);
        }

        @Override
        public void deleteEventTimeTimer(long time) {
            throw new UnsupportedOperationException(UNSUPPORTED_DELETE_TIMER_MSG);
        }
    }
}
