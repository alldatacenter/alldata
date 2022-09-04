package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ComponentPackageTaskDOMapper {

    long countByExample(ComponentPackageTaskDOExample example);

    int deleteByExample(ComponentPackageTaskDOExample example);

    int insertSelective(ComponentPackageTaskDO record);

    List<ComponentPackageTaskDO> selectByExampleWithBLOBs(ComponentPackageTaskDOExample example);

    List<ComponentPackageTaskDO> selectByExample(ComponentPackageTaskDOExample example);

    int updateByExampleSelective(@Param("record") ComponentPackageTaskDO record, @Param("example") ComponentPackageTaskDOExample example);
}