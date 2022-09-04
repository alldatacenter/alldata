package com.alibaba.tesla.appmanager.common.enums;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;

/**
 * ComponentPackage 部署单状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum ComponentPackageTaskStateEnum {

    /**
     * 部署单已创建
     */
    CREATED,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 成功
     */
    SUCCESS,

    /**
     * 失败
     */
    FAILURE,

    /**
     * 已跳过
     */
    SKIP;

    /**
     * 状态流转，将当前状态根据 Event 流转到下一个状态
     *
     * @param event 事件
     * @return 下一个合法状态
     */
    public ComponentPackageTaskStateEnum next(ComponentPackageTaskEventEnum event) {
        switch (this) {
            case CREATED:
                if (ComponentPackageTaskEventEnum.START.equals(event)) {
                    return RUNNING;
                }
                break;
            case RUNNING:
                if (ComponentPackageTaskEventEnum.SUCCEED.equals(event)) {
                    return SUCCESS;
                } else if (ComponentPackageTaskEventEnum.SKIPPED.equals(event)) {
                    return SKIP;
                } else if (ComponentPackageTaskEventEnum.FAILED.equals(event)) {
                    return FAILURE;
                } else if (ComponentPackageTaskEventEnum.OP_RETRY.equals(event)) {
                    return RUNNING;
                }
                break;
            case FAILURE:
                if (ComponentPackageTaskEventEnum.OP_RETRY.equals(event)) {
                    return RUNNING;
                }
                return null;
            default:
                break;
        }
        throw new AppException(AppErrorCode.STATE_MACHINE_ERROR,
            String.format("Cannot transform state %s on event %s", this, event));
    }
}
