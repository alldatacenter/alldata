package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ProductQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductDO;

import java.util.List;

public interface ProductRepository {
    long countByCondition(ProductQueryCondition condition);

    int deleteByCondition(ProductQueryCondition condition);

    int insert(ProductDO record);

    List<ProductDO> selectByCondition(ProductQueryCondition condition);

    ProductDO getByCondition(ProductQueryCondition condition);

    int updateByCondition(ProductDO record, ProductQueryCondition condition);
}