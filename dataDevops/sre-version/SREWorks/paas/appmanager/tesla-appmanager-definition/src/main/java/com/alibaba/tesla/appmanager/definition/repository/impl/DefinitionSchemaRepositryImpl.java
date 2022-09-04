package com.alibaba.tesla.appmanager.definition.repository.impl;

import com.alibaba.tesla.appmanager.definition.repository.DefinitionSchemaRepositry;
import com.alibaba.tesla.appmanager.definition.repository.condition.DefinitionSchemaQueryCondition;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDOExample;
import com.alibaba.tesla.appmanager.definition.repository.mapper.DefinitionSchemaMapper;
import com.alibaba.tesla.appmanager.common.util.DateUtil;
import com.github.pagehelper.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class DefinitionSchemaRepositryImpl implements DefinitionSchemaRepositry {

    @Autowired
    private DefinitionSchemaMapper definitionSchemaMapper;

    @Override
    public long countByCondition(DefinitionSchemaQueryCondition condition) {
        return definitionSchemaMapper.countByExample(buildExample(condition));
    }

    @Override
    public int deleteByCondition(DefinitionSchemaQueryCondition condition) {
        return definitionSchemaMapper.deleteByExample(buildExample(condition));
    }

    @Override
    public int deleteByPrimaryKey(Long id) {
        return definitionSchemaMapper.deleteByPrimaryKey(id);
    }

    @Override
    public int insert(DefinitionSchemaDO record) {
        return definitionSchemaMapper.insertSelective(insertDate(record));
    }

    @Override
    public List<DefinitionSchemaDO> selectByCondition(DefinitionSchemaQueryCondition condition) {
        condition.doPagination();
        return definitionSchemaMapper.selectByExample(buildExample(condition));
    }

    @Override
    public DefinitionSchemaDO selectByPrimaryKey(Long id) {
        return definitionSchemaMapper.selectByPrimaryKey(id);
    }

    @Override
    public int updateByCondition(DefinitionSchemaDO record, DefinitionSchemaQueryCondition condition) {
        return definitionSchemaMapper.updateByExampleSelective(updateDate(record), buildExample(condition));
    }

    @Override
    public int updateByPrimaryKey(DefinitionSchemaDO record) {
        return definitionSchemaMapper.updateByPrimaryKeySelective(updateDate(record));
    }

    private DefinitionSchemaDOExample buildExample(DefinitionSchemaQueryCondition condition) {
        DefinitionSchemaDOExample example = new DefinitionSchemaDOExample();
        DefinitionSchemaDOExample.Criteria criteria = example.createCriteria();
        if (StringUtils.isNotBlank(condition.getName())) {
            criteria.andNameEqualTo(condition.getName());
        }
        return example;
    }

    private DefinitionSchemaDO insertDate(DefinitionSchemaDO record) {
        Date now = DateUtil.now();
        record.setGmtCreate(now);
        record.setGmtModified(now);
        return record;
    }

    private DefinitionSchemaDO updateDate(DefinitionSchemaDO record) {
        Date now = DateUtil.now();
        record.setGmtModified(now);
        return record;
    }
}
