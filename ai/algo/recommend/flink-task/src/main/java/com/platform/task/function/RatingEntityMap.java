package com.platform.schedule.function;

import com.platform.schedule.entity.RatingEntity;
import org.apache.flink.api.common.functions.MapFunction;

public class RatingEntityMap implements MapFunction<String, RatingEntity> {
    @Override
    public RatingEntity map(String s) throws Exception {
        RatingEntity ratingEntity = new RatingEntity();
        String[] tmp = s.split(",");
        ratingEntity.setUserId(Integer.parseInt(tmp[0]));
        ratingEntity.setProductId(Integer.parseInt(tmp[1]));
        ratingEntity.setScore(Double.parseDouble(tmp[2]));
        ratingEntity.setTimestamp(Integer.parseInt(tmp[3]));
        System.out.println(ratingEntity);
        return ratingEntity;
    }
}
