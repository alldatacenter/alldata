package com.platform.schedule.function;

import com.platform.schedule.entity.MysqlClient;
import com.platform.schedule.entity.RatingEntity;
import org.apache.flink.api.common.functions.MapFunction;

public class DataLoaderMapFunction2 implements MapFunction<RatingEntity, RatingEntity> {
    @Override
    public RatingEntity map(RatingEntity ratingEntity) throws Exception {
        System.out.println(ratingEntity);
        if(ratingEntity != null) {
            MysqlClient.putData(ratingEntity);
        }
        return ratingEntity;
    }
}