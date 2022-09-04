package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppAttrDO;
import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppAttrDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeployAppAttrDOMapper {
    long countByExample(DeployAppAttrDOExample example);

    int deleteByExample(DeployAppAttrDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(DeployAppAttrDO record);

    int insertSelective(DeployAppAttrDO record);

    List<DeployAppAttrDO> selectByExample(DeployAppAttrDOExample example);

    DeployAppAttrDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") DeployAppAttrDO record, @Param("example") DeployAppAttrDOExample example);

    int updateByExample(@Param("record") DeployAppAttrDO record, @Param("example") DeployAppAttrDOExample example);

    int updateByPrimaryKeySelective(DeployAppAttrDO record);

    int updateByPrimaryKey(DeployAppAttrDO record);
}