package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageComponentRelDOExample;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface AppPackageComponentRelDOMapper {
    long countByExample(AppPackageComponentRelDOExample example);

    int deleteByExample(AppPackageComponentRelDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AppPackageComponentRelDO record);

    int insertSelective(AppPackageComponentRelDO record);

    List<AppPackageComponentRelDO> selectByExampleWithRowbounds(AppPackageComponentRelDOExample example, RowBounds rowBounds);

    List<AppPackageComponentRelDO> selectByExample(AppPackageComponentRelDOExample example);

    AppPackageComponentRelDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AppPackageComponentRelDO record, @Param("example") AppPackageComponentRelDOExample example);

    int updateByExample(@Param("record") AppPackageComponentRelDO record, @Param("example") AppPackageComponentRelDOExample example);

    int updateByPrimaryKeySelective(AppPackageComponentRelDO record);

    int updateByPrimaryKey(AppPackageComponentRelDO record);
}