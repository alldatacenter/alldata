package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseTaskQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskDO;

import java.util.List;

public interface ProductReleaseTaskRepository {
    long countByCondition(ProductReleaseTaskQueryCondition condition);

    int deleteByCondition(ProductReleaseTaskQueryCondition condition);

    int insert(ProductReleaseTaskDO record);

    List<ProductReleaseTaskDO> selectByCondition(ProductReleaseTaskQueryCondition condition);

    ProductReleaseTaskDO getByCondition(ProductReleaseTaskQueryCondition condition);

    int updateByCondition(ProductReleaseTaskDO record, ProductReleaseTaskQueryCondition condition);
}