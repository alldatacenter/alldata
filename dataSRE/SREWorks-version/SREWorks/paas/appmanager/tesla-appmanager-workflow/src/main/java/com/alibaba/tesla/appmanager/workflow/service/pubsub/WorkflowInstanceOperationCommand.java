package com.alibaba.tesla.appmanager.workflow.service.pubsub;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Workflow Instance 操作命令
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowInstanceOperationCommand {

    /**
     * 可用命令
     */
    public static final String COMMAND_TERMINATE = "TERMINATE";
    public static final String COMMAND_RESUME = "RESUME";
    public static final String COMMAND_RETRY = "RETRY";

    /**
     * 命令 (TERMINATE/RETRY/RESUME)
     */
    private String command;

    /**
     * 要操作的 Workflow Instance ID 对象
     */
    private Long workflowInstanceId;
}
