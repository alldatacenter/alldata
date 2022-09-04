package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeployComponentDOMapper {
    long countByExample(DeployComponentDOExample example);

    int deleteByExample(DeployComponentDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(DeployComponentDO record);

    int insertSelective(DeployComponentDO record);

    List<DeployComponentDO> selectByExample(DeployComponentDOExample example);

    DeployComponentDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") DeployComponentDO record, @Param("example") DeployComponentDOExample example);

    int updateByExample(@Param("record") DeployComponentDO record, @Param("example") DeployComponentDOExample example);

    int updateByPrimaryKeySelective(DeployComponentDO record);

    int updateByPrimaryKey(DeployComponentDO record);
}