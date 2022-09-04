package com.alibaba.tesla.appmanager.workflow.action;

import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowInstanceDO;

/**
 * Workflow Instance State 处理 Action 接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface WorkflowInstanceStateAction {

    /**
     * 自身处理逻辑
     *
     * @param instance Workflow 实例
     */
    void run(WorkflowInstanceDO instance);
}
