package com.platform.schedule.function;

import com.platform.schedule.entity.RatingEntity;
import org.apache.flink.api.common.functions.AggregateFunction;

public class AggCount implements AggregateFunction<RatingEntity, Long, Long> {
    @Override
    public Long createAccumulator() {
        return 0L;
    }

    @Override
    public Long add(RatingEntity ratingEntity, Long aLong) {
        return aLong + 1;
    }

    @Override
    public Long getResult(Long aLong) {
        return aLong;
    }

    @Override
    public Long merge(Long aLong, Long acc1) {
        return aLong + acc1;
    }
}
