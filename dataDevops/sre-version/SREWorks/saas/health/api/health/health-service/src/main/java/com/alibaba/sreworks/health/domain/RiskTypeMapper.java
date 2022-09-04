package com.alibaba.sreworks.health.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface RiskTypeMapper {
    long countByExample(RiskTypeExample example);

    int deleteByExample(RiskTypeExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(RiskType record);

    int insertSelective(RiskType record);

    List<RiskType> selectByExampleWithRowbounds(RiskTypeExample example, RowBounds rowBounds);

    List<RiskType> selectByExample(RiskTypeExample example);

    RiskType selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") RiskType record, @Param("example") RiskTypeExample example);

    int updateByExample(@Param("record") RiskType record, @Param("example") RiskTypeExample example);

    int updateByPrimaryKeySelective(RiskType record);

    int updateByPrimaryKey(RiskType record);
}