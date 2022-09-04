package com.alibaba.tesla.appmanager.server.repository.mapper;

import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDO;
import com.alibaba.tesla.appmanager.server.repository.domain.AppMetaDOExample;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AppMetaDOMapper {
    long countByExample(AppMetaDOExample example);

    int deleteByExample(AppMetaDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AppMetaDO record);

    int insertSelective(AppMetaDO record);

    List<AppMetaDO> selectByExample(AppMetaDOExample example);

    AppMetaDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AppMetaDO record, @Param("example") AppMetaDOExample example);

    int updateByExample(@Param("record") AppMetaDO record, @Param("example") AppMetaDOExample example);

    int updateByPrimaryKeySelective(AppMetaDO record);

    int updateByPrimaryKey(AppMetaDO record);
}