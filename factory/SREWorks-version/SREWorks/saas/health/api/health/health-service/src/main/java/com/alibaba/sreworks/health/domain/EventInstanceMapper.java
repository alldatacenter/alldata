package com.alibaba.sreworks.health.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface EventInstanceMapper {
    long countByExample(EventInstanceExample example);

    int deleteByExample(EventInstanceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(EventInstance record);

    int insertSelective(EventInstance record);

    List<EventInstance> selectByExampleWithBLOBsWithRowbounds(EventInstanceExample example, RowBounds rowBounds);

    List<EventInstance> selectByExampleWithBLOBs(EventInstanceExample example);

    List<EventInstance> selectByExampleWithRowbounds(EventInstanceExample example, RowBounds rowBounds);

    List<EventInstance> selectByExample(EventInstanceExample example);

    EventInstance selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") EventInstance record, @Param("example") EventInstanceExample example);

    int updateByExampleWithBLOBs(@Param("record") EventInstance record, @Param("example") EventInstanceExample example);

    int updateByExample(@Param("record") EventInstance record, @Param("example") EventInstanceExample example);

    int updateByPrimaryKeySelective(EventInstance record);

    int updateByPrimaryKeyWithBLOBs(EventInstance record);

    int updateByPrimaryKey(EventInstance record);

    int batchInsert(@Param("list") List<EventInstance> list);
}