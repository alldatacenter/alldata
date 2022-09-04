package com.alibaba.tesla.appmanager.deployconfig.repository.mapper;

import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDO;
import com.alibaba.tesla.appmanager.deployconfig.repository.domain.DeployConfigDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeployConfigDOMapper {
    long countByExample(DeployConfigDOExample example);

    int deleteByExample(DeployConfigDOExample example);

    int insertSelective(DeployConfigDO record);

    List<DeployConfigDO> selectByExample(DeployConfigDOExample example);

    int updateByExampleSelective(@Param("record") DeployConfigDO record, @Param("example") DeployConfigDOExample example);
}