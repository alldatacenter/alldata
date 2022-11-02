package com.alibaba.sreworks.health.domain;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Map;

public interface IncidentInstanceMapper {
    long countByExample(IncidentInstanceExample example);

    int deleteByExample(IncidentInstanceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(IncidentInstance record);

    int insertSelective(IncidentInstance record);

    List<IncidentInstance> selectByExampleWithBLOBsWithRowbounds(IncidentInstanceExample example, RowBounds rowBounds);

    List<IncidentInstance> selectByExampleWithBLOBs(IncidentInstanceExample example);

    List<IncidentInstance> selectByExampleWithRowbounds(IncidentInstanceExample example, RowBounds rowBounds);

    List<IncidentInstance> selectByExample(IncidentInstanceExample example);

    IncidentInstance selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") IncidentInstance record, @Param("example") IncidentInstanceExample example);

    int updateByExampleWithBLOBs(@Param("record") IncidentInstance record, @Param("example") IncidentInstanceExample example);

    int updateByExample(@Param("record") IncidentInstance record, @Param("example") IncidentInstanceExample example);

    int updateByPrimaryKeySelective(IncidentInstance record);

    int updateByPrimaryKeyWithBLOBs(IncidentInstance record);

    int updateByPrimaryKey(IncidentInstance record);

    List<JSONObject> countGroupByTime(Map<String, Object> params);
}