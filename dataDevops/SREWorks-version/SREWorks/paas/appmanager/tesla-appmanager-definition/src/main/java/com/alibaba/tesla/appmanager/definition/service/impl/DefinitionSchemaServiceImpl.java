package com.alibaba.tesla.appmanager.definition.service.impl;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.definition.repository.DefinitionSchemaRepositry;
import com.alibaba.tesla.appmanager.definition.repository.condition.DefinitionSchemaQueryCondition;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;
import com.alibaba.tesla.appmanager.definition.service.DefinitionSchemaService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Function;

@Service
@Slf4j
public class DefinitionSchemaServiceImpl implements DefinitionSchemaService {

    @Autowired
    private DefinitionSchemaRepositry definitionSchemaRepository;

    /**
     * 根据指定条件查询对应的 definition schema 列表
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of DefinitionSchema
     */
    @Override
    public Pagination<DefinitionSchemaDO> list(DefinitionSchemaQueryCondition condition, String operator) {
        List<DefinitionSchemaDO> result = definitionSchemaRepository.selectByCondition(condition);
        return Pagination.valueOf(result, Function.identity());
    }

    /**
     * 根据指定条件查询对应的 definition schema 列表 (期望仅返回一个)
     *
     * @param condition 查询条件
     * @param operator  操作人
     * @return DefinitionSchema DTO, 不存在则返回 null，存在多个则抛出异常
     */
    @Override
    public DefinitionSchemaDO get(DefinitionSchemaQueryCondition condition, String operator) {
        List<DefinitionSchemaDO> results = definitionSchemaRepository.selectByCondition(condition);
        if (results.size() == 0) {
            return null;
        } else if (results.size() > 1) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "multiple records found in database");
        } else {
            return results.get(0);
        }
    }

    /**
     * 向系统中新增或更新一个 Definition Schema
     *
     * @param record   请求记录
     * @param operator 操作人
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void apply(DefinitionSchemaDO record, String operator) {
        assert !StringUtils.isEmpty(record.getName());
        String name = record.getName();
        DefinitionSchemaQueryCondition condition = DefinitionSchemaQueryCondition.builder().name(name).build();
        List<DefinitionSchemaDO> schemaList = definitionSchemaRepository.selectByCondition(condition);
        assert schemaList.size() <= 1;
        if (schemaList.size() == 0) {
            definitionSchemaRepository.insert(record);
            log.info("definition schema has inserted into db|name={}", name);
        } else {
            definitionSchemaRepository.updateByCondition(record, condition);
            log.info("definition schema has updated|name={}", name);
        }
    }

    /**
     * 删除指定条件的 Definition Schema
     *
     * @param condition 条件
     * @param operator  操作人
     * @return 删除的数量 (0 or 1)
     */
    @Override
    public int delete(DefinitionSchemaQueryCondition condition, String operator) {
        return definitionSchemaRepository.deleteByCondition(condition);
    }
}
