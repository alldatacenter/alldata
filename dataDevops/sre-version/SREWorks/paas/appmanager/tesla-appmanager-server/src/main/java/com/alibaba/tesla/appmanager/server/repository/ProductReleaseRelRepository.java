package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseRelDO;

import java.util.List;

public interface ProductReleaseRelRepository {
    long countByCondition(ProductReleaseRelQueryCondition condition);

    int deleteByCondition(ProductReleaseRelQueryCondition condition);

    int insert(ProductReleaseRelDO record);

    List<ProductReleaseRelDO> selectByCondition(ProductReleaseRelQueryCondition condition);

    ProductReleaseRelDO getByCondition(ProductReleaseRelQueryCondition condition);

    int updateByCondition(ProductReleaseRelDO record, ProductReleaseRelQueryCondition condition);
}