package com.alibaba.tesla.appmanager.common.enums;

/**
 * Workflow 任务状态枚举
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum WorkflowTaskStateEnum {

    /**
     * 等待执行中
     */
    PENDING,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 完成 UserFunction 后等待部署单完成中
     */
    WAITING,

    /**
     * RUNNING 状态暂停等待中
     */
    RUNNING_SUSPEND,

    /**
     * WAITING 状态暂停等待中
     */
    WAITING_SUSPEND,

    /**
     * 成功
     */
    SUCCESS,

    /**
     * 失败
     */
    FAILURE,

    /**
     * 异常
     */
    EXCEPTION,

    /**
     * 被终止
     */
    TERMINATED;

    public WorkflowTaskStateEnum next(WorkflowTaskEventEnum event) {
        switch (this) {
            case PENDING:
                if (WorkflowTaskEventEnum.START.equals(event)) {
                    return RUNNING;
                } else if (WorkflowTaskEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                }
                break;
            case RUNNING:
                if (WorkflowTaskEventEnum.PAUSE.equals(event)) {
                    return RUNNING_SUSPEND;
                } else if (WorkflowTaskEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                } else if (WorkflowTaskEventEnum.PROCESS_FINISHED.equals(event)) {
                    return WAITING;
                } else if (WorkflowTaskEventEnum.PROCESS_FAILED.equals(event)) {
                    return FAILURE;
                } else if (WorkflowTaskEventEnum.PROCESS_UNKNOWN_ERROR.equals(event)) {
                    return EXCEPTION;
                } else if (WorkflowTaskEventEnum.PROCESS_SUSPEND.equals(event)) {
                    return WAITING_SUSPEND;
                }
                break;
            case RUNNING_SUSPEND:
                if (WorkflowTaskEventEnum.RESUME.equals(event)) {
                    return RUNNING;
                } else if (WorkflowTaskEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                }
                break;
            case WAITING:
                if (WorkflowTaskEventEnum.PAUSE.equals(event)) {
                    return WAITING_SUSPEND;
                } else if (WorkflowTaskEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                } else if (WorkflowTaskEventEnum.WAITING_FINISHED.equals(event)) {
                    return SUCCESS;
                } else if (WorkflowTaskEventEnum.WAITING_FAILED.equals(event)) {
                    return FAILURE;
                } else if (WorkflowTaskEventEnum.WAITING_UNKNOWN_ERROR.equals(event)) {
                    return EXCEPTION;
                } else if (WorkflowTaskEventEnum.TRIGGER_UPDATE.equals(event)) {
                    return WAITING;
                }
                break;
            case WAITING_SUSPEND:
                if (WorkflowTaskEventEnum.RESUME.equals(event)) {
                    return WAITING;
                } else if (WorkflowTaskEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                }
                break;
            case SUCCESS:
            case FAILURE:
            case EXCEPTION:
                if (WorkflowTaskEventEnum.OP_RETRY.equals(event)) {
                    return RUNNING;
                }
                break;
            case TERMINATED:
                if (WorkflowTaskEventEnum.OP_RETRY.equals(event)) {
                    return RUNNING;
                } else if (WorkflowTaskEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                }
                break;
        }
        return null;
    }

    public boolean isFinalState() {
        return this.equals(SUCCESS) || this.equals(FAILURE) || this.equals(EXCEPTION) || this.equals(TERMINATED);
    }
}
