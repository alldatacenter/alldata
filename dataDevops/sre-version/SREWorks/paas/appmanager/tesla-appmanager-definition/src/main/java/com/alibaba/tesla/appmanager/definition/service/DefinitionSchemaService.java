package com.alibaba.tesla.appmanager.definition.service;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.definition.repository.condition.DefinitionSchemaQueryCondition;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;

/**
 * Definition Schema 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DefinitionSchemaService {

    /**
     * 根据指定条件查询对应的 definition schema 列表
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of DefinitionSchema
     */
    Pagination<DefinitionSchemaDO> list(DefinitionSchemaQueryCondition condition, String operator);

    /**
     * 根据指定条件查询对应的 definition schema (期望只返回一个)
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of DefinitionSchema
     */
    DefinitionSchemaDO get(DefinitionSchemaQueryCondition condition, String operator);

    /**
     * 向系统中新增或更新一个 Definition Schema
     *
     * @param request   记录的值
     * @param operator 操作人
     */
    void apply(DefinitionSchemaDO request, String operator);

    /**
     * 删除指定条件的 Definition Schema
     *
     * @param condition 条件
     * @param operator  操作人
     * @return 删除的数量 (0 or 1)
     */
    int delete(DefinitionSchemaQueryCondition condition, String operator);
}
