package com.alibaba.sreworks.health.domain;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Map;

public interface RiskInstanceMapper {
    long countByExample(RiskInstanceExample example);

    int deleteByExample(RiskInstanceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(RiskInstance record);

    int insertSelective(RiskInstance record);

    List<RiskInstance> selectByExampleWithBLOBsWithRowbounds(RiskInstanceExample example, RowBounds rowBounds);

    List<RiskInstance> selectByExampleWithBLOBs(RiskInstanceExample example);

    List<RiskInstance> selectByExampleWithRowbounds(RiskInstanceExample example, RowBounds rowBounds);

    List<RiskInstance> selectByExample(RiskInstanceExample example);

    RiskInstance selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") RiskInstance record, @Param("example") RiskInstanceExample example);

    int updateByExampleWithBLOBs(@Param("record") RiskInstance record, @Param("example") RiskInstanceExample example);

    int updateByExample(@Param("record") RiskInstance record, @Param("example") RiskInstanceExample example);

    int updateByPrimaryKeySelective(RiskInstance record);

    int updateByPrimaryKeyWithBLOBs(RiskInstance record);

    int updateByPrimaryKey(RiskInstance record);

    int batchInsert(@Param("list") List<RiskInstance> list);

    List<JSONObject> countGroupByTime(Map<String, Object> params);
}