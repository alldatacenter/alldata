package com.platform.manage.mapper;

import com.platform.manage.model.PmsProductLadder;
import com.platform.manage.model.PmsProductLadderExample;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface PmsProductLadderMapper {
    long countByExample(PmsProductLadderExample example);

    int deleteByExample(PmsProductLadderExample example);

    int deleteByPrimaryKey(Long id);

    int insert(PmsProductLadder record);

    int insertSelective(PmsProductLadder record);

    List<PmsProductLadder> selectByExample(PmsProductLadderExample example);

    PmsProductLadder selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") PmsProductLadder record, @Param("example") PmsProductLadderExample example);

    int updateByExample(@Param("record") PmsProductLadder record, @Param("example") PmsProductLadderExample example);

    int updateByPrimaryKeySelective(PmsProductLadder record);

    int updateByPrimaryKey(PmsProductLadder record);
}