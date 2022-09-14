package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppPackageTagDOExample;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface AppPackageTagDOMapper {
    long countByExample(AppPackageTagDOExample example);

    int deleteByExample(AppPackageTagDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AppPackageTagDO record);

    int insertSelective(AppPackageTagDO record);

    List<AppPackageTagDO> selectByExampleWithRowbounds(AppPackageTagDOExample example, RowBounds rowBounds);

    List<AppPackageTagDO> selectByExample(AppPackageTagDOExample example);

    AppPackageTagDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AppPackageTagDO record, @Param("example") AppPackageTagDOExample example);

    int updateByExample(@Param("record") AppPackageTagDO record, @Param("example") AppPackageTagDOExample example);

    int updateByPrimaryKeySelective(AppPackageTagDO record);

    int updateByPrimaryKey(AppPackageTagDO record);
}