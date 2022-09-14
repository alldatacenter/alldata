package com.alibaba.tesla.appmanager.deployconfig.repository.mapper;

import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigHistoryDO;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigHistoryDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeployConfigHistoryDOMapper {
    long countByExample(DeployConfigHistoryDOExample example);

    int deleteByExample(DeployConfigHistoryDOExample example);

    int insertSelective(DeployConfigHistoryDO record);

    List<DeployConfigHistoryDO> selectByExample(DeployConfigHistoryDOExample example);

    int updateByExampleSelective(@Param("record") DeployConfigHistoryDO record, @Param("example") DeployConfigHistoryDOExample example);
}