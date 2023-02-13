package com.alibaba.tesla.appmanager.server.action;

import com.alibaba.tesla.appmanager.server.repository.domain.ComponentPackageTaskDO;

/**
 * ComponentPackage 的 State 处理 Action 接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface ComponentPackageTaskStateAction {

    /**
     * 自身逻辑处理
     *
     * @param task 任务详情
     */
    void run(ComponentPackageTaskDO task);
}
