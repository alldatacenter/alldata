package com.alibaba.tesla.appmanager.domain.core;

import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;

/**
 * 满足 Workload 引用规则的接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface WorkloadBinder {

    /**
     * 获取当前被绑定的 workload resource 的引用对象
     *
     * @return WorkloadResource
     */
    WorkloadResource getWorkloadRef();

    /**
     * 设置 Component 对象
     *
     * @param component Component
     */
    void setComponent(DeployAppSchema.SpecComponent component);

    /**
     * 设置 Owner Reference
     *
     * @param ownerReference Owner Reference JSON String
     */
    void setOwnerReference(String ownerReference);

    /**
     * 返回 Owner Reference 字符串
     *
     * @return Owner Reference JSON Object String
     */
    String getOwnerReference();

    /**
     * 获取当前被绑定的 Component 对象
     *
     * @return Component
     */
    DeployAppSchema.SpecComponent getComponent();
}
