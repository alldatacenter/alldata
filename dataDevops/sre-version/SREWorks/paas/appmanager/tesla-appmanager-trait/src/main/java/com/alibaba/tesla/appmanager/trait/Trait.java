package com.alibaba.tesla.appmanager.trait;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.core.TaskExecutor;
import com.alibaba.tesla.appmanager.domain.core.WorkloadBinder;

/**
 * Trait 接口定义
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface Trait extends TaskExecutor, WorkloadBinder {

    /**
     * 获取当前 Trait 的 spec 定义（一般被内部修改）
     *
     * @return JSONObject
     */
    JSONObject getSpec();
}
