package com.alibaba.sreworks.health.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface FailureRecordMapper {
    long countByExample(FailureRecordExample example);

    int deleteByExample(FailureRecordExample example);

    int deleteByPrimaryKey(Long id);

    int insert(FailureRecord record);

    int insertSelective(FailureRecord record);

    List<FailureRecord> selectByExampleWithBLOBsWithRowbounds(FailureRecordExample example, RowBounds rowBounds);

    List<FailureRecord> selectByExampleWithBLOBs(FailureRecordExample example);

    List<FailureRecord> selectByExampleWithRowbounds(FailureRecordExample example, RowBounds rowBounds);

    List<FailureRecord> selectByExample(FailureRecordExample example);

    FailureRecord selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") FailureRecord record, @Param("example") FailureRecordExample example);

    int updateByExampleWithBLOBs(@Param("record") FailureRecord record, @Param("example") FailureRecordExample example);

    int updateByExample(@Param("record") FailureRecord record, @Param("example") FailureRecordExample example);

    int updateByPrimaryKeySelective(FailureRecord record);

    int updateByPrimaryKeyWithBLOBs(FailureRecord record);

    int updateByPrimaryKey(FailureRecord record);
}