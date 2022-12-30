package com.alibaba.tesla.appmanager.trait.service.handler;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.req.trait.TraitExecuteReq;
import com.alibaba.tesla.appmanager.domain.res.trait.TraitExecuteRes;
import com.alibaba.tesla.appmanager.dynamicscript.core.GroovyHandler;

/**
 * Trait Handler
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface TraitHandler extends GroovyHandler {

    /**
     * Reconcile 过程
     *
     * @param payload 请求参数
     */
    default void reconcile(JSONObject payload) {
    }

    /**
     * Trait 业务侧逻辑执行
     *
     * @param request Trait 输入参数
     * @return Trait 修改后的 Spec 定义
     */
    default TraitExecuteRes execute(TraitExecuteReq request) {
        return TraitExecuteRes.builder()
                .spec(request.getSpec())
                .build();
    }
}
