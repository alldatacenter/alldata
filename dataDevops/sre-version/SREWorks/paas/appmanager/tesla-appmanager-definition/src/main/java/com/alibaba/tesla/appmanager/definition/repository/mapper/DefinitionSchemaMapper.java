package com.alibaba.tesla.appmanager.definition.repository.mapper;

import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDOExample;

import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DefinitionSchemaMapper {
    long countByExample(DefinitionSchemaDOExample example);

    int deleteByExample(DefinitionSchemaDOExample example);

    int deleteByPrimaryKey(Long id);

    int insert(DefinitionSchemaDO record);

    int insertSelective(DefinitionSchemaDO record);

    List<DefinitionSchemaDO> selectByExample(DefinitionSchemaDOExample example);

    DefinitionSchemaDO selectByPrimaryKey(Long id);

    int updateByExampleSelective(@Param("record") DefinitionSchemaDO record, @Param("example") DefinitionSchemaDOExample example);

    int updateByExample(@Param("record") DefinitionSchemaDO record, @Param("example") DefinitionSchemaDOExample example);

    int updateByPrimaryKeySelective(DefinitionSchemaDO record);

    int updateByPrimaryKey(DefinitionSchemaDO record);
}