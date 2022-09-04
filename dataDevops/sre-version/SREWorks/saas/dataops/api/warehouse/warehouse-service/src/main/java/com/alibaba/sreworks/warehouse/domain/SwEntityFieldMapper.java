package com.alibaba.sreworks.warehouse.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface SwEntityFieldMapper {
    long countByExample(SwEntityFieldExample example);

    int deleteByExample(SwEntityFieldExample example);

    int deleteByPrimaryKey(Long id);

    int insert(SwEntityField record);

    int insertSelective(SwEntityField record);

    List<SwEntityField> selectByExampleWithBLOBsWithRowbounds(SwEntityFieldExample example, RowBounds rowBounds);

    List<SwEntityField> selectByExampleWithBLOBs(SwEntityFieldExample example);

    List<SwEntityField> selectByExampleWithRowbounds(SwEntityFieldExample example, RowBounds rowBounds);

    List<SwEntityField> selectByExample(SwEntityFieldExample example);

    SwEntityField selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") SwEntityField record, @Param("example") SwEntityFieldExample example);

    int updateByExampleWithBLOBs(@Param("record") SwEntityField record, @Param("example") SwEntityFieldExample example);

    int updateByExample(@Param("record") SwEntityField record, @Param("example") SwEntityFieldExample example);

    int updateByPrimaryKeySelective(SwEntityField record);

    int updateByPrimaryKeyWithBLOBs(SwEntityField record);

    int updateByPrimaryKey(SwEntityField record);
}