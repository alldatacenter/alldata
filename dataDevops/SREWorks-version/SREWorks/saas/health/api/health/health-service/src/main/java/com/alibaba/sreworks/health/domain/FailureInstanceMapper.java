package com.alibaba.sreworks.health.domain;

import com.alibaba.fastjson.JSONObject;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;
import java.util.Map;

public interface FailureInstanceMapper {
    long countByExample(FailureInstanceExample example);

    int deleteByExample(FailureInstanceExample example);

    int deleteByPrimaryKey(Long id);

    int insert(FailureInstance record);

    int insertSelective(FailureInstance record);

    List<FailureInstance> selectByExampleWithBLOBsWithRowbounds(FailureInstanceExample example, RowBounds rowBounds);

    List<FailureInstance> selectByExampleWithBLOBs(FailureInstanceExample example);

    List<FailureInstance> selectByExampleWithRowbounds(FailureInstanceExample example, RowBounds rowBounds);

    List<FailureInstance> selectByExample(FailureInstanceExample example);

    FailureInstance selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") FailureInstance record, @Param("example") FailureInstanceExample example);

    int updateByExampleWithBLOBs(@Param("record") FailureInstance record, @Param("example") FailureInstanceExample example);

    int updateByExample(@Param("record") FailureInstance record, @Param("example") FailureInstanceExample example);

    int updateByPrimaryKeySelective(FailureInstance record);

    int updateByPrimaryKeyWithBLOBs(FailureInstance record);

    int updateByPrimaryKey(FailureInstance record);

    List<JSONObject> countGroupByTime(Map<String, Object> params);
}