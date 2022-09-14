package com.alibaba.tesla.appmanager.domain.res.workflow;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow Instance 操作结果
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceOperationRes {

    /**
     * 命令 (TERMINATE/RETRY/RESUME)
     */
    private String command;

    /**
     * 要操作的 Workflow Instance ID 对象
     */
    private Long workflowInstanceId;

    /**
     * 执行操作机器
     */
    private String clientHost;

    /**
     * 是否成功
     */
    private boolean success;

    /**
     * 附加信息
     */
    private String message;
}
