package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ComponentPackageDOMapper {
    long countByExample(ComponentPackageDOExample example);

    int deleteByExample(ComponentPackageDOExample example);

    int deleteByPrimaryKey(Long id);

    int insertSelective(ComponentPackageDO record);

    List<ComponentPackageDO> selectByExampleWithBLOBs(ComponentPackageDOExample example);

    List<ComponentPackageDO> selectByExample(ComponentPackageDOExample example);

    ComponentPackageDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") ComponentPackageDO record, @Param("example") ComponentPackageDOExample example);

    int updateByPrimaryKeySelective(ComponentPackageDO record);
}