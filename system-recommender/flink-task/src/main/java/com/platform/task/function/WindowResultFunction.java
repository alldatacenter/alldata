package com.platform.task.function;

import com.platform.task.entity.TopEntity;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.functions.windowing.WindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class WindowResultFunction implements WindowFunction<Long, TopEntity, Tuple, TimeWindow> {
    @Override
    public void apply(Tuple tuple, TimeWindow timeWindow, Iterable<Long> iterable, Collector<TopEntity> collector)
            throws Exception {
        Integer productId = tuple.getField(0);
        Long count = iterable.iterator().next();
        collector.collect(TopEntity.getTopEntity(productId, timeWindow.getEnd(), count));
    }
}


