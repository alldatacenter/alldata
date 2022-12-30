package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTaskDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppPackageTaskDOMapper {
    long countByExample(AppPackageTaskDOExample example);

    int deleteByExample(AppPackageTaskDOExample example);

    int insertSelective(AppPackageTaskDO record);

    List<AppPackageTaskDO> selectByExampleWithBLOBs(AppPackageTaskDOExample example);

    List<AppPackageTaskDO> selectByExample(AppPackageTaskDOExample example);

    int updateByExampleSelective(@Param("record") AppPackageTaskDO record, @Param("example") AppPackageTaskDOExample example);
}