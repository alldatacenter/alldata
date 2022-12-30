package com.alibaba.tesla.appmanager.trait.service;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.common.pagination.Pagination;
import com.alibaba.tesla.appmanager.trait.repository.condition.TraitQueryCondition;
import com.alibaba.tesla.appmanager.trait.repository.domain.TraitDO;

/**
 * Trait 服务接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface TraitService {

    /**
     * 根据指定条件查询对应的 trait 列表
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of Trait
     */
    Pagination<TraitDO> list(TraitQueryCondition condition, String operator);

    /**
     * 根据指定条件查询对应的 trait (期望只返回一个)
     *
     * @param condition 条件
     * @param operator  操作人
     * @return Page of Trait
     */
    TraitDO get(TraitQueryCondition condition, String operator);

    /**
     * 向系统中新增或更新一个 trait
     *
     * @param request  记录的值
     * @param operator 操作人
     */
    void apply(TraitDO request, String operator);

    /**
     * 删除指定条件的 trait
     *
     * @param condition 条件
     * @param operator  操作人
     * @return 删除的数量 (0 or 1)
     */
    int delete(TraitQueryCondition condition, String operator);

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
