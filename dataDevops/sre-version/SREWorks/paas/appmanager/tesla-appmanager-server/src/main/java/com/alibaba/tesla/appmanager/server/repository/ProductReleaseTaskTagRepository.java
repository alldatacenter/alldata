package com.alibaba.tesla.appmanager.server.repository;

import com.alibaba.tesla.appmanager.server.repository.condition.ProductReleaseTaskTagQueryCondition;
import com.alibaba.tesla.appmanager.server.repository.domain.ProductReleaseTaskTagDO;

import java.util.List;

public interface ProductReleaseTaskTagRepository {
    long countByCondition(ProductReleaseTaskTagQueryCondition condition);

    int deleteByCondition(ProductReleaseTaskTagQueryCondition condition);

    int insert(ProductReleaseTaskTagDO record);

    List<ProductReleaseTaskTagDO> selectByCondition(ProductReleaseTaskTagQueryCondition condition);
}