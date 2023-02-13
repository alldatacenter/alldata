package com.alibaba.tesla.appmanager.server.action;

import com.alibaba.tesla.appmanager.server.repository.domain.DeployComponentDO;

import java.util.Map;

/**
 * Component 部署工单 State 处理 Action 接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface DeployComponentStateAction {

    /**
     * 自身逻辑处理
     *
     * @param subOrder 部署工单
     * @param attrMap  属性字典
     */
    void run(DeployComponentDO subOrder, Map<String, String> attrMap);
}
