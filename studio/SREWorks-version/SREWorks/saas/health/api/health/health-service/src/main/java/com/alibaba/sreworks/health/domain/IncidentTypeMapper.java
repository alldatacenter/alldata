package com.alibaba.sreworks.health.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface IncidentTypeMapper {
    long countByExample(IncidentTypeExample example);

    int deleteByExample(IncidentTypeExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(IncidentType record);

    int insertSelective(IncidentType record);

    List<IncidentType> selectByExampleWithRowbounds(IncidentTypeExample example, RowBounds rowBounds);

    List<IncidentType> selectByExample(IncidentTypeExample example);

    IncidentType selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") IncidentType record, @Param("example") IncidentTypeExample example);

    int updateByExample(@Param("record") IncidentType record, @Param("example") IncidentTypeExample example);

    int updateByPrimaryKeySelective(IncidentType record);

    int updateByPrimaryKey(IncidentType record);
}