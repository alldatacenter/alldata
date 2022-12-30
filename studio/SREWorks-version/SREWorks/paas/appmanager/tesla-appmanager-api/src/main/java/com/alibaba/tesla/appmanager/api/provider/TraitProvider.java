package com.alibaba.tesla.appmanager.api.provider;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.domain.dto.TraitDTO;
import com.alibaba.tesla.appmanager.domain.req.trait.TraitQueryReq;

/**
 * Trait 服务
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface TraitProvider {

    /**
     * 根据指定条件查询对应的 Trait 列表
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return PageInfo of Trait DTO
     */
    Pagination<TraitDTO> list(TraitQueryReq request, String operator);

    /**
     * 根据指定条件查询对应的 Trait 列表 (期望仅返回一个)
     *
     * @param request  查询请求
     * @param operator 操作人
     * @return Trait DTO, 不存在则返回 null，存在多个则抛出异常
     */
    TraitDTO get(TraitQueryReq request, String operator);

    /**
     * 向系统中新增或更新一个 Trait
     *
     * @param request  记录的值
     * @param operator 操作人
     */
    void apply(String request, String operator);

    /**
     * 删除指定条件的 Trait
     *
     * @param request  条件
     * @param operator 操作人
     * @return 删除的数量 (0 or 1)
     */
    int delete(TraitQueryReq request, String operator);

    /**
     * 调用指定 Trait 的 reconcile 过程
     *
     * @param name    Trait 唯一名称
     * @param payload 请求 Payload
     */
    void reconcile(String name, JSONObject payload);

    /**
     * 调用指定 Trait 的 reconcile 过程
     *
     * @param name   Trait 唯一名称
     * @param object Reconcile Object
     * @param properties Reconcile Properties
     */
    void reconcile(String name, JSONObject object, JSONObject properties);
}
