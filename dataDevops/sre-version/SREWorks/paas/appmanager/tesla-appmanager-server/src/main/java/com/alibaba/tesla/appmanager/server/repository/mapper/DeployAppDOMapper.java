package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeployAppDOMapper {
    long countByExample(DeployAppDOExample example);

    int deleteByExample(DeployAppDOExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(DeployAppDO record);

    List<DeployAppDO> selectByExample(DeployAppDOExample example);

    List<DeployAppDO> selectByExampleAndOption(
            @Param("example") DeployAppDOExample example,
            @Param("key") String key,
            @Param("value") String value);

    DeployAppDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") DeployAppDO record, @Param("example") DeployAppDOExample example);

    int updateByPrimaryKeySelective(DeployAppDO record);
}