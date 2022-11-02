package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseAppRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseAppRelDO;

import java.util.List;

public interface ProductReleaseAppRelRepository {
    long countByCondition(ProductReleaseAppRelQueryCondition condition);

    int deleteByCondition(ProductReleaseAppRelQueryCondition condition);

    int insert(ProductReleaseAppRelDO record);

    List<ProductReleaseAppRelDO> selectByCondition(ProductReleaseAppRelQueryCondition condition);

    ProductReleaseAppRelDO getByCondition(ProductReleaseAppRelQueryCondition condition);

    int updateByCondition(ProductReleaseAppRelDO record, ProductReleaseAppRelQueryCondition condition);
}