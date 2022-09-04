package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseSchedulerQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseSchedulerDO;

import java.util.List;

public interface ProductReleaseSchedulerRepository {
    long countByCondition(ProductReleaseSchedulerQueryCondition condition);

    int deleteByCondition(ProductReleaseSchedulerQueryCondition condition);

    int insert(ProductReleaseSchedulerDO record);

    List<ProductReleaseSchedulerDO> selectByCondition(ProductReleaseSchedulerQueryCondition condition);

    ProductReleaseSchedulerDO getByCondition(ProductReleaseSchedulerQueryCondition condition);

    int updateByCondition(ProductReleaseSchedulerDO record, ProductReleaseSchedulerQueryCondition condition);
}