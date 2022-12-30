package com.alibaba.tesla.appmanager.common.enums;

/**
 * Component 部署单状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum DeployComponentStateEnum {

    /**
     * 部署单已创建
     */
    CREATED,

    /**
     * 处理中
     */
    PROCESSING,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 等待人工操作
     */
    WAIT_FOR_OP,

    /**
     * 失败
     */
    FAILURE,

    /**
     * 成功
     */
    SUCCESS,

    /**
     * 未知异常
     */
    EXCEPTION;

    /**
     * 状态流转，将当前状态根据 Event 流转到下一个状态
     *
     * @param event 事件
     * @return 下一个合法状态
     */
    public DeployComponentStateEnum next(DeployComponentEventEnum event) {
        switch (this) {
            case CREATED:
                if (DeployComponentEventEnum.START.equals(event)) {
                    return PROCESSING;
                }
                break;
            case PROCESSING:
                if (DeployComponentEventEnum.PROCESS_FINISHED.equals(event)) {
                    return RUNNING;
                } else if (DeployComponentEventEnum.FLOW_FAILED.equals(event)) {
                    return WAIT_FOR_OP;
                } else if (DeployComponentEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case RUNNING:
                if (DeployComponentEventEnum.FLOW_SUCCEED.equals(event)) {
                    return SUCCESS;
                } else if (DeployComponentEventEnum.FLOW_FAILED.equals(event)) {
                    return WAIT_FOR_OP;
                } else if (DeployComponentEventEnum.TRIGGER_UPDATE.equals(event)) {
                    return RUNNING;
                } else if (DeployComponentEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case WAIT_FOR_OP:
                if (DeployComponentEventEnum.OP_RETRY.equals(event)) {
                    return PROCESSING;
                } else if (DeployComponentEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case EXCEPTION:
                if (DeployComponentEventEnum.OP_RETRY.equals(event)) {
                    return PROCESSING;
                } else if (DeployComponentEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            default:
                return null;
        }
        return null;
    }
}
