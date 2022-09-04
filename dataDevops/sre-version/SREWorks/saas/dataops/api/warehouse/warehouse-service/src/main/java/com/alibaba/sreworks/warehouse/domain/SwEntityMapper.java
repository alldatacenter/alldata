package com.alibaba.sreworks.warehouse.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface SwEntityMapper {
    long countByExample(SwEntityExample example);

    int deleteByExample(SwEntityExample example);

    int deleteByPrimaryKey(Long id);

    int insert(SwEntity record);

    int insertSelective(SwEntity record);

    List<SwEntity> selectByExampleWithBLOBsWithRowbounds(SwEntityExample example, RowBounds rowBounds);

    List<SwEntity> selectByExampleWithBLOBs(SwEntityExample example);

    List<SwEntity> selectByExampleWithRowbounds(SwEntityExample example, RowBounds rowBounds);

    List<SwEntity> selectByExample(SwEntityExample example);

    SwEntity selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") SwEntity record, @Param("example") SwEntityExample example);

    int updateByExampleWithBLOBs(@Param("record") SwEntity record, @Param("example") SwEntityExample example);

    int updateByExample(@Param("record") SwEntity record, @Param("example") SwEntityExample example);

    int updateByPrimaryKeySelective(SwEntity record);

    int updateByPrimaryKeyWithBLOBs(SwEntity record);

    int updateByPrimaryKey(SwEntity record);
}