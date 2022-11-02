package com.alibaba.tesla.appmanager.server.action;

import com.alibaba.tesla.appmanager.server.repository.domain.DeployAppDO;

import java.util.Map;

/**
 * App 部署工单 State 处理 Action 接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DeployAppStateAction {

    /**
     * 自身逻辑处理
     *
     * @param order   部署工单
     * @param attrMap 扩展属性字典
     */
    void run(DeployAppDO order, Map<String, String> attrMap);
}
