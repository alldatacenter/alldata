package com.alibaba.tesla.appmanager.definition.repository;

import com.alibaba.tesla.appmanager.definition.repository.condition.DefinitionSchemaQueryCondition;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;
import com.github.pagehelper.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface DefinitionSchemaRepositry {

    long countByCondition(DefinitionSchemaQueryCondition condition);

    int deleteByCondition(DefinitionSchemaQueryCondition condition);

    int deleteByPrimaryKey(Long id);

    int insert(DefinitionSchemaDO record);

    List<DefinitionSchemaDO> selectByCondition(DefinitionSchemaQueryCondition condition);

    DefinitionSchemaDO selectByPrimaryKey(Long id);

    int updateByCondition(@Param("record") DefinitionSchemaDO record, @Param("Condition") DefinitionSchemaQueryCondition condition);

    int updateByPrimaryKey(DefinitionSchemaDO record);
}