package com.platform.schedule.function;

import com.platform.schedule.entity.HbaseClient;
import com.platform.schedule.entity.RatingEntity;
import org.apache.flink.api.common.functions.MapFunction;

public class DataToHbaseMapFunction implements MapFunction<RatingEntity, RatingEntity> {
    @Override
    public RatingEntity map(RatingEntity ratingEntity) throws Exception {
        if(ratingEntity != null) {
            String rowkey =ratingEntity.getProductId() + "_" + ratingEntity.getUserId() + "_" + ratingEntity.getTimestamp();
            System.out.println(rowkey);
            // record user rate info
            HbaseClient.putData("rating", rowkey, "p", "productId", String.valueOf(ratingEntity.getProductId()));
            HbaseClient.putData("rating", rowkey, "p", "userId", String.valueOf(ratingEntity.getUserId()));
            HbaseClient.putData("rating", rowkey, "p", "score", String.valueOf(ratingEntity.getScore()));
            HbaseClient.putData("rating", rowkey, "p", "timestamp", String.valueOf(ratingEntity.getTimestamp()));
            // record user-product info
            HbaseClient.increamColumn("userProduct", String.valueOf(ratingEntity.getUserId()), "product", String.valueOf(ratingEntity.getProductId()));
            // record product-user info
            HbaseClient.increamColumn("productUser", String.valueOf(ratingEntity.getProductId()), "user", String.valueOf(ratingEntity.getUserId()));
        }
        return ratingEntity;
    }
}
