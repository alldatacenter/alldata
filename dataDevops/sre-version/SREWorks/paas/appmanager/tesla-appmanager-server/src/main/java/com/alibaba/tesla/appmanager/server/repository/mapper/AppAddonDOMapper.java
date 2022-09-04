package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppAddonDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppAddonDOMapper {
    long countByExample(AppAddonDOExample example);

    int deleteByExample(AppAddonDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AppAddonDO record);

    int insertSelective(AppAddonDO record);

    List<AppAddonDO> selectByExample(AppAddonDOExample example);

    AppAddonDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AppAddonDO record, @Param("example") AppAddonDOExample example);

    int updateByExample(@Param("record") AppAddonDO record, @Param("example") AppAddonDOExample example);

    int updateByPrimaryKeySelective(AppAddonDO record);

    int updateByPrimaryKey(AppAddonDO record);
}