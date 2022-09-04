package com.alibaba.tesla.appmanager.workflow.action;

import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;

/**
 * Workflow Task State 处理 Action 接口
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public interface WorkflowTaskStateAction {

    /**
     * 自身处理逻辑
     *
     * @param task Workflow Task
     */
    void run(WorkflowTaskDO task);
}
