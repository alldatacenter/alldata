package com.platform.mall.mapper.admin;

import com.platform.mall.entity.admin.UmsGrowthChangeHistory;
import com.platform.mall.entity.admin.UmsGrowthChangeHistoryExample;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface UmsGrowthChangeHistoryMapper {
    long countByExample(UmsGrowthChangeHistoryExample example);

    int deleteByExample(UmsGrowthChangeHistoryExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UmsGrowthChangeHistory record);

    int insertSelective(UmsGrowthChangeHistory record);

    List<UmsGrowthChangeHistory> selectByExample(UmsGrowthChangeHistoryExample example);

    UmsGrowthChangeHistory selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") UmsGrowthChangeHistory record, @Param("example") UmsGrowthChangeHistoryExample example);

    int updateByExample(@Param("record") UmsGrowthChangeHistory record, @Param("example") UmsGrowthChangeHistoryExample example);

    int updateByPrimaryKeySelective(UmsGrowthChangeHistory record);

    int updateByPrimaryKey(UmsGrowthChangeHistory record);
}
