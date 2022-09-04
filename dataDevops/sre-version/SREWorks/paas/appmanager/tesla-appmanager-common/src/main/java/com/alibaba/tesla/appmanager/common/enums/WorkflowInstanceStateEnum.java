package com.alibaba.tesla.appmanager.common.enums;

/**
 * Workflow 实例状态 Enum
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum WorkflowInstanceStateEnum {

    /**
     * 等待执行中
     */
    PENDING,

    /**
     * 预处理中
     */
    PREPROCESSING,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 暂停等待中
     */
    SUSPEND,

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

    /**
     * 状态流转，将当前状态根据 Event 流转到下一个状态
     *
     * @param event 事件
     * @return 下一个合法状态
     */
    public WorkflowInstanceStateEnum next(WorkflowInstanceEventEnum event) {
        switch (this) {
            case PENDING:
                if (WorkflowInstanceEventEnum.START.equals(event)) {
                    return PREPROCESSING;
                } else if (WorkflowInstanceEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                }
                break;
            case PREPROCESSING:
                if (WorkflowInstanceEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                } else if (WorkflowInstanceEventEnum.PREPROCESS_FINISHED.equals(event)) {
                    return RUNNING;
                }
                break;
            case RUNNING:
                if (WorkflowInstanceEventEnum.PAUSE.equals(event)) {
                    return SUSPEND;
                } else if (WorkflowInstanceEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                } else if (WorkflowInstanceEventEnum.PROCESS_FINISHED.equals(event)) {
                    return SUCCESS;
                } else if (WorkflowInstanceEventEnum.PROCESS_FAILED.equals(event)) {
                    return FAILURE;
                } else if (WorkflowInstanceEventEnum.PROCESS_UNKNOWN_ERROR.equals(event)) {
                    return EXCEPTION;
                }
                break;
            case SUSPEND:
                if (WorkflowInstanceEventEnum.RESUME.equals(event)) {
                    return RUNNING;
                } else if (WorkflowInstanceEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                } else if (WorkflowInstanceEventEnum.PROCESS_FINISHED.equals(event)) {
                    return SUCCESS;
                } else if (WorkflowInstanceEventEnum.PROCESS_FAILED.equals(event)) {
                    return FAILURE;
                } else if (WorkflowInstanceEventEnum.PROCESS_UNKNOWN_ERROR.equals(event)) {
                    return EXCEPTION;
                }
                break;
            case SUCCESS:
            case FAILURE:
            case EXCEPTION:
                if (WorkflowInstanceEventEnum.OP_RETRY.equals(event)) {
                    return RUNNING;
                }
                break;
            case TERMINATED:
                if (WorkflowInstanceEventEnum.OP_RETRY.equals(event)) {
                    return RUNNING;
                } else if (WorkflowInstanceEventEnum.PAUSE.equals(event)) {
                    return SUSPEND;
                } else if (WorkflowInstanceEventEnum.OP_TERMINATE.equals(event)) {
                    return TERMINATED;
                } else if (WorkflowInstanceEventEnum.PROCESS_FINISHED.equals(event)) {
                    return SUCCESS;
                } else if (WorkflowInstanceEventEnum.PROCESS_FAILED.equals(event)) {
                    return FAILURE;
                } else if (WorkflowInstanceEventEnum.PROCESS_UNKNOWN_ERROR.equals(event)) {
                    return EXCEPTION;
                }
                break;
        }
        return null;
    }

    public boolean isErrorState() {
        return this.equals(FAILURE) || this.equals(EXCEPTION) || this.equals(TERMINATED);
    }

    public boolean isFinalState() {
        return this.equals(SUCCESS) || this.equals(FAILURE) || this.equals(EXCEPTION) || this.equals(TERMINATED);
    }
}
