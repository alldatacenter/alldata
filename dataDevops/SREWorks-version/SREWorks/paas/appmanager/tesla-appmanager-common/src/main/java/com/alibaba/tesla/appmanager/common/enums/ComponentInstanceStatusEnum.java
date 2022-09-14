package com.alibaba.tesla.appmanager.common.enums;

import com.alibaba.tesla.appmanager.common.exception.AppErrorCode;
import com.alibaba.tesla.appmanager.common.exception.AppException;

/**
 * 组件实例状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum ComponentInstanceStatusEnum {

    /**
     * 尚未实现
     */
    NOT_IMPLEMENTED,

    /**
     * 已过期
     */
    EXPIRED,

    /**
     * 进入等待状态
     */
    PENDING,

    /**
     * 正在运行中
     */
    RUNNING,

    /**
     * 准备升级中
     */
    PREPARING_UPDATE,

    /**
     * 升级中
     */
    UPDATING,

    /**
     * 准备删除中
     */
    PREPARING_DELETE,

    /**
     * 正在运行中，但出现 WARNING
     */
    WARNING,

    /**
     * 正在运行中，但出现 ERROR
     */
    ERROR,

    /**
     * 完成态（运行成功）
     */
    COMPLETED,

    /**
     * 完成态（运行失败）
     */
    FAILED,

    /**
     * 未知异常
     */
    UNKNOWN;

    /**
     * 判断从 fromStatus -> this 的状态变化是否会导致当前状态的 stable -> unstable 变化
     *
     * @param fromStatus 前状态
     * @return true if unstable
     */
    public boolean checkUnstable(ComponentInstanceStatusEnum fromStatus) {
        if (fromStatus == null) {
            throw new AppException(AppErrorCode.INVALID_USER_ARGS, "empty fromStatus");
        }

        // 对所有的不稳定状态，看下是否变迁而来，如果是，那么返回 true
        switch (this) {
            case PENDING:
            case WARNING:
            case ERROR:
            case UPDATING:
            case PREPARING_DELETE:
            case PREPARING_UPDATE:
                return !fromStatus.equals(this);
            default:
                return false;
        }
    }
}
