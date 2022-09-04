package com.alibaba.sreworks.warehouse.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface SwModelFieldMapper {
    long countByExample(SwModelFieldExample example);

    int deleteByExample(SwModelFieldExample example);

    int deleteByPrimaryKey(Long id);

    int insert(SwModelField record);

    int insertSelective(SwModelField record);

    List<SwModelField> selectByExampleWithBLOBsWithRowbounds(SwModelFieldExample example, RowBounds rowBounds);

    List<SwModelField> selectByExampleWithBLOBs(SwModelFieldExample example);

    List<SwModelField> selectByExampleWithRowbounds(SwModelFieldExample example, RowBounds rowBounds);

    List<SwModelField> selectByExample(SwModelFieldExample example);

    SwModelField selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") SwModelField record, @Param("example") SwModelFieldExample example);

    int updateByExampleWithBLOBs(@Param("record") SwModelField record, @Param("example") SwModelFieldExample example);

    int updateByExample(@Param("record") SwModelField record, @Param("example") SwModelFieldExample example);

    int updateByPrimaryKeySelective(SwModelField record);

    int updateByPrimaryKeyWithBLOBs(SwModelField record);

    int updateByPrimaryKey(SwModelField record);
}