package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseTaskAppPackageTaskRelQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskAppPackageTaskRelDO;

import java.util.List;

public interface ProductReleaseTaskAppPackageTaskRelRepository {
    long countByCondition(ProductReleaseTaskAppPackageTaskRelQueryCondition condition);

    int deleteByCondition(ProductReleaseTaskAppPackageTaskRelQueryCondition condition);

    int insert(ProductReleaseTaskAppPackageTaskRelDO record);

    List<ProductReleaseTaskAppPackageTaskRelDO> selectByCondition(ProductReleaseTaskAppPackageTaskRelQueryCondition condition);
}