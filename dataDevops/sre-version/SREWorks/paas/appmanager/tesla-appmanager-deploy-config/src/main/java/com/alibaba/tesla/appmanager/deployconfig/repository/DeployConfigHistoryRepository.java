package com.alibaba.tesla.appmanager.deployconfig.repository;

import com.alibaba.tesla.appmanager.deployconfig.repository.condition.DeployConfigHistoryQueryCondition;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigHistoryDO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DeployConfigHistoryRepository {

    long countByExample(DeployConfigHistoryQueryCondition condition);

    int deleteByExample(DeployConfigHistoryQueryCondition condition);

    int insertSelective(DeployConfigHistoryDO record);

    List<DeployConfigHistoryDO> selectByExample(DeployConfigHistoryQueryCondition condition);

    int updateByExampleSelective(@Param("record") DeployConfigHistoryDO record, @Param("condition") DeployConfigHistoryQueryCondition condition);
}
