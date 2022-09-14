package com.alibaba.tesla.appmanager.workflow.service.thread;

import com.alibaba.tesla.appmanager.domain.res.workflow.ExecuteWorkflowHandlerRes;
import com.alibaba.tesla.appmanager.workflow.repository.domain.WorkflowTaskDO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 执行工作流任务时，用于主线程和子线程交互结果的对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExecuteWorkflowTaskResult {

    /**
     * 入参: 工作流任务对象
     */
    private WorkflowTaskDO task;

    /**
     * 外部状态: 是否被终止
     */
    private boolean terminated;

    /**
     * 外部状态: 是否被暂停
     */
    private boolean paused;

    /**
     * 出参: 执行是否成功
     */
    private boolean success;

    /**
     * 出参: 执行扩展信息 (出错/暂停/终止时携带额外信息)
     */
    private String extMessage;

    /**
     * 出参: Workflow Task 执行成功时的返回结果 (仅 success==true 时有意义)
     */
    private ExecuteWorkflowHandlerRes output;
}