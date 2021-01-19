package com.platform.task.function;

import com.platform.task.util.MysqlClient;
import com.platform.task.entity.ProductEntity;
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

