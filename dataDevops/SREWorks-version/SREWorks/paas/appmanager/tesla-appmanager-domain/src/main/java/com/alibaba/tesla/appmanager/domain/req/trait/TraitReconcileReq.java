package com.alibaba.tesla.appmanager.domain.req.trait;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trait Reconcile 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TraitReconcileReq {

    /**
     * Reconcile Object 信息 (NamespacedName)
     */
    private JSONObject object;

    /**
     * Reconcile Properties
     */
    private JSONObject properties;
}
