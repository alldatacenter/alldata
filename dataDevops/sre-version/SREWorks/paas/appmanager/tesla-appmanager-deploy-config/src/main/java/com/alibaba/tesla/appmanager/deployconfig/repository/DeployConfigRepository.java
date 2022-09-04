package com.alibaba.tesla.appmanager.deployconfig.repository;

import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployConfigRepository {

    long countByCondition(DeployConfigQueryCondition condition);

    int deleteByCondition(DeployConfigQueryCondition condition);

    int insert(DeployConfigDO record);

    List<DeployConfigDO> selectByCondition(DeployConfigQueryCondition condition);

    int updateByCondition(@Param("record") DeployConfigDO record, @Param("condition") DeployConfigQueryCondition condition);
}
