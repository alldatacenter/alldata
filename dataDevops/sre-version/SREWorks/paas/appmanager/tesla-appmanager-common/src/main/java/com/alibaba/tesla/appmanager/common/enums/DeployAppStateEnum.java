package com.alibaba.tesla.appmanager.common.enums;

/**
 * App 部署单状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum DeployAppStateEnum {

    /**
     * 部署单已创建
     */
    CREATED,

    /**
     * 前置检查中(检查备份是否存在)
     */
    BACKUP_CHECKING,

    /**
     * 组件包是否存在的检测逻辑
     */
    COMPONENT_CHECKING,

    /**
     * 创建组件中
     */
    PROCESSING_COMPONENT,

    /**
     * 等待创建组件包完成
     */
    WAITING_COMPONENT,

    /**
     * 备份中
     */
    BACKING_UP,

    /**
     * 处理中
     */
    PROCESSING,

    /**
     * 运行中
     */
    WAITING,

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
    public DeployAppStateEnum next(DeployAppEventEnum event) {
        switch (this) {
            case CREATED:
                if (DeployAppEventEnum.START.equals(event)) {
                    return COMPONENT_CHECKING;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case COMPONENT_CHECKING:
                if (DeployAppEventEnum.COMPONENTS_EXISTS.equals(event)) {
                    return BACKUP_CHECKING;
                } else if (DeployAppEventEnum.COMPONENTS_NOT_FOUND.equals(event)) {
                    return PROCESSING_COMPONENT;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case PROCESSING_COMPONENT:
                if (DeployAppEventEnum.COMPONENTS_TASK_CREATED.equals(event)) {
                    return WAITING_COMPONENT;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case WAITING_COMPONENT:
                if (DeployAppEventEnum.COMPONENTS_FINISHED.equals(event)) {
                    return BACKUP_CHECKING;
                } else if (DeployAppEventEnum.COMPONENTS_FAILED.equals(event)) {
                    return FAILURE;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case BACKUP_CHECKING:
                if (DeployAppEventEnum.APP_PACKAGE_EXIST.equals(event)) {
                    return PROCESSING;
                } else if (DeployAppEventEnum.APP_PACKAGE_NOT_FOUND.equals(event)) {
                    return BACKING_UP;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case BACKING_UP:
                if (DeployAppEventEnum.BACKUP_SUCCEED.equals(event)) {
                    return PROCESSING;
                } else if (DeployAppEventEnum.BACKUP_FAILED.equals(event)) {
                    return FAILURE;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case PROCESSING:
                if (DeployAppEventEnum.PROCESS_FINISHED.equals(event)) {
                    return WAITING;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case WAITING:
                if (DeployAppEventEnum.ALL_SUCCEED.equals(event)) {
                    return SUCCESS;
                } else if (DeployAppEventEnum.PARTIAL_FAILED.equals(event)) {
                    return WAIT_FOR_OP;
                } else if (DeployAppEventEnum.TRIGGER_UPDATE.equals(event)) {
                    return WAITING;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                }
                break;
            case WAIT_FOR_OP:
                if (DeployAppEventEnum.OP_ROLLBACK.equals(event)) {
                    return FAILURE;
                } else if (DeployAppEventEnum.OP_TERMINATE.equals(event)) {
                    return FAILURE;
                } else if (DeployAppEventEnum.OP_RETRY.equals(event)) {
                    return WAITING;
                }
                break;
            default:
                return null;
        }
        return null;
    }

    public Boolean isError(){
        return this.equals(FAILURE) || this.equals(EXCEPTION) || this.equals(WAIT_FOR_OP);
    }
}
