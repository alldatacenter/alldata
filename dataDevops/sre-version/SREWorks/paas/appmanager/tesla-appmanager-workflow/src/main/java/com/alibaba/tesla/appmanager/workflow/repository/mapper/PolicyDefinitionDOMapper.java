package com.alibaba.tesla.appmanager.workflow.repository.mapper;

import com.alibaba.tesla.appmanager.workflow.repository.domain.PolicyDefinitionDO;
import com.alibaba.tesla.appmanager.workflow.repository.domain.PolicyDefinitionDOExample;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface PolicyDefinitionDOMapper {
    long countByExample(PolicyDefinitionDOExample example);

    int deleteByExample(PolicyDefinitionDOExample example);

    int insertSelective(PolicyDefinitionDO record);

    List<PolicyDefinitionDO> selectByExample(PolicyDefinitionDOExample example);

    int updateByExampleSelective(@Param("record") PolicyDefinitionDO record, @Param("example") PolicyDefinitionDOExample example);
}