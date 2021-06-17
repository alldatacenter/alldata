package com.platform.schedule.function;

import com.platform.schedule.entity.MysqlClient;
import com.platform.schedule.entity.ProductEntity;
import org.apache.flink.api.common.functions.MapFunction;

public class DataLoaderMapFunction implements MapFunction<ProductEntity, ProductEntity> {
    @Override
    public ProductEntity map(ProductEntity productEntity) throws Exception {
        if(productEntity != null) {
            MysqlClient.putData(productEntity);
        }
        return productEntity;
    }
}

