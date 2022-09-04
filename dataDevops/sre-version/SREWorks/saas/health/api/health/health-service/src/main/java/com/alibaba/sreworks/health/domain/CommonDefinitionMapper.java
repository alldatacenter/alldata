package com.alibaba.sreworks.health.domain;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.session.RowBounds;

import java.util.List;

public interface CommonDefinitionMapper {
    long countByExample(CommonDefinitionExample example);

    int deleteByExample(CommonDefinitionExample example);

    int deleteByPrimaryKey(Integer id);

    int insert(CommonDefinition record);

    int insertSelective(CommonDefinition record);

    List<CommonDefinition> selectByExampleWithRowbounds(CommonDefinitionExample example, RowBounds rowBounds);

    List<CommonDefinition> selectByExample(CommonDefinitionExample example);

    CommonDefinition selectByPrimaryKey(Integer id);

    int updateByExampleSelective(@Param("record") CommonDefinition record, @Param("example") CommonDefinitionExample example);

    int updateByExample(@Param("record") CommonDefinition record, @Param("example") CommonDefinitionExample example);

    int updateByPrimaryKeySelective(CommonDefinition record);

    int updateByPrimaryKey(CommonDefinition record);

    List<CommonDefinitionGroupCount> countGroupByCategory();
}