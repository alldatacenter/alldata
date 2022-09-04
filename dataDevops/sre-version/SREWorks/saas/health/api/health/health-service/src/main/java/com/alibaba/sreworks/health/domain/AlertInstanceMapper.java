package com.alibaba.sreworks.health.domain;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Map;

public interface AlertInstanceMapper {
    long countByExample(AlertInstanceExample example);

    int deleteByExample(AlertInstanceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(AlertInstance record);

    int insertSelective(AlertInstance record);

    List<AlertInstance> selectByExampleWithBLOBsWithRowbounds(AlertInstanceExample example, RowBounds rowBounds);

    List<AlertInstance> selectByExampleWithBLOBs(AlertInstanceExample example);

    List<AlertInstance> selectByExampleWithRowbounds(AlertInstanceExample example, RowBounds rowBounds);

    List<AlertInstance> selectByExample(AlertInstanceExample example);

    AlertInstance selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") AlertInstance record, @Param("example") AlertInstanceExample example);

    int updateByExampleWithBLOBs(@Param("record") AlertInstance record, @Param("example") AlertInstanceExample example);

    int updateByExample(@Param("record") AlertInstance record, @Param("example") AlertInstanceExample example);

    int updateByPrimaryKeySelective(AlertInstance record);

    int updateByPrimaryKeyWithBLOBs(AlertInstance record);

    int updateByPrimaryKey(AlertInstance record);

    int batchInsert(@Param("list") List<AlertInstance> list);

    List<JSONObject> countGroupByTime(Map<String, Object> params);
}