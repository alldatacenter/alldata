package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.DefinitionSchemaDTO;
import com.alibaba.tesla.appmanager.domain.req.DefinitionSchemaQueryReq;

/**
 * Definition Schema 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DefinitionSchemaProvider {

    /**
     * 根据指定条件查询对应的 definition schema 列表
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return PageInfo of DefinitionSchema DTO
     */
    Pagination<DefinitionSchemaDTO> list(DefinitionSchemaQueryReq request, String operator);

    /**
     * 根据指定条件查询对应的 definition schema 列表 (期望仅返回一个)
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return DefinitionSchema DTO, 不存在则返回 null，存在多个则抛出异常
     */
    DefinitionSchemaDTO get(DefinitionSchemaQueryReq request, String operator);

    /**
     * 向系统中新增或更新一个 Definition Schema
     *
     * @param request  记录的值
     * @param operator 操作人
     */
    void apply(DefinitionSchemaDTO request, String operator);

    /**
     * 删除指定条件的 Definition Schema
     *
     * @param condition 条件
     * @param operator  操作人
     * @return 删除的数量 (0 or 1)
     */
    int delete(DefinitionSchemaQueryReq condition, String operator);
}
