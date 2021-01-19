package com.platform.task.function;

import com.platform.task.util.MysqlClient;
import com.platform.task.entity.RatingEntity;
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