package com.platform.manage.mapper;

import com.platform.manage.model.UmsIntegrationChangeHistory;
import com.platform.manage.model.UmsIntegrationChangeHistoryExample;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface UmsIntegrationChangeHistoryMapper {
    long countByExample(UmsIntegrationChangeHistoryExample example);

    int deleteByExample(UmsIntegrationChangeHistoryExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UmsIntegrationChangeHistory record);

    int insertSelective(UmsIntegrationChangeHistory record);

    List<UmsIntegrationChangeHistory> selectByExample(UmsIntegrationChangeHistoryExample example);

    UmsIntegrationChangeHistory selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") UmsIntegrationChangeHistory record, @Param("example") UmsIntegrationChangeHistoryExample example);

    int updateByExample(@Param("record") UmsIntegrationChangeHistory record, @Param("example") UmsIntegrationChangeHistoryExample example);

    int updateByPrimaryKeySelective(UmsIntegrationChangeHistory record);

    int updateByPrimaryKey(UmsIntegrationChangeHistory record);
}