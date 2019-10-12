package com.platform.mbg.mapper;

import com.platform.mbg.model.UmsMemberTask;
import com.platform.mbg.model.UmsMemberTaskExample;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface UmsMemberTaskMapper {
    long countByExample(UmsMemberTaskExample example);

    int deleteByExample(UmsMemberTaskExample example);

    int deleteByPrimaryKey(Long id);

    int insert(UmsMemberTask record);

    int insertSelective(UmsMemberTask record);

    List<UmsMemberTask> selectByExample(UmsMemberTaskExample example);

    UmsMemberTask selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") UmsMemberTask record, @Param("example") UmsMemberTaskExample example);

    int updateByExample(@Param("record") UmsMemberTask record, @Param("example") UmsMemberTaskExample example);

    int updateByPrimaryKeySelective(UmsMemberTask record);

    int updateByPrimaryKey(UmsMemberTask record);
}