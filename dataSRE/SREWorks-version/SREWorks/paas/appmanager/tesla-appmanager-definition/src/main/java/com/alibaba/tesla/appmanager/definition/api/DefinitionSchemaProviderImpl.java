package com.alibaba.tesla.appmanager.definition.api;

import com.alibaba.tesla.appmanager.api.provider.DefinitionSchemaProvider;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.definition.assembly.DefinitionSchemaDtoConvert;
import com.alibaba.tesla.appmanager.definition.repository.condition.DefinitionSchemaQueryCondition;
import com.alibaba.tesla.appmanager.definition.repository.domain.DefinitionSchemaDO;
import com.alibaba.tesla.appmanager.definition.service.DefinitionSchemaService;
import com.alibaba.tesla.appmanager.domain.dto.DefinitionSchemaDTO;
import com.alibaba.tesla.appmanager.domain.req.DefinitionSchemaQueryReq;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * API 服务层实现 - Definition Schema
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Service
@Slf4j
public class DefinitionSchemaProviderImpl implements DefinitionSchemaProvider {

    @Autowired
    private DefinitionSchemaService definitionSchemaService;

    @Autowired
    private DefinitionSchemaDtoConvert definitionSchemaDtoConvert;

    /**
     * 根据指定条件查询对应的 definition schema 列表
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return PageInfo of DefinitionSchema DTO
     */
    @Override
    public Pagination<DefinitionSchemaDTO> list(DefinitionSchemaQueryReq request, String operator) {
        DefinitionSchemaQueryCondition condition = DefinitionSchemaQueryCondition.builder()
            .name(request.getName())
            .build();
        Pagination<DefinitionSchemaDO> results = definitionSchemaService.list(condition, operator);
        return Pagination.transform(results, item -> definitionSchemaDtoConvert.to(item));
    }

    /**
     * 根据指定条件查询对应的 definition schema 列表 (期望仅返回一个)
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return DefinitionSchema DTO, 不存在则返回 null，存在多个则抛出异常
     */
    @Override
    public DefinitionSchemaDTO get(DefinitionSchemaQueryReq request, String operator) {
        DefinitionSchemaQueryCondition condition = DefinitionSchemaQueryCondition.builder()
            .name(request.getName())
            .build();
        DefinitionSchemaDO result = definitionSchemaService.get(condition, operator);
        return definitionSchemaDtoConvert.to(result);
    }

    /**
     * 向系统中新增或更新一个 Definition Schema
     *
     * @param request  记录的值
     * @param operator 操作人
     */
    @Override
    public void apply(DefinitionSchemaDTO request, String operator) {
        definitionSchemaService.apply(definitionSchemaDtoConvert.from(request), operator);
    }

    /**
     * 删除指定条件的 Definition Schema
     *
     * @param request 条件
     * @param operator  操作人
     * @return 删除的数量 (0 or 1)
     */
    @Override
    public int delete(DefinitionSchemaQueryReq request, String operator) {
        DefinitionSchemaQueryCondition condition = DefinitionSchemaQueryCondition.builder()
            .name(request.getName())
            .build();
        return definitionSchemaService.delete(condition, operator);
    }
}
