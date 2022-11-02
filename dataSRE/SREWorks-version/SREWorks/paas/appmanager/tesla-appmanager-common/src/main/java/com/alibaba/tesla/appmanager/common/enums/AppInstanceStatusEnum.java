package com.alibaba.tesla.appmanager.common.enums;

/**
 * 应用实例状态 Enum
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum AppInstanceStatusEnum {

    /**
     * 进入状态
     */
    PENDING,

    /**
     * 正在运行中
     */
    RUNNING,

    /**
     * 升级中
     */
    UPDATING,

    /**
     * 正在运行中，但出现 WARNING
     */
    WARNING,

    /**
     * 正在运行中，但出现 ERROR
     */
    ERROR,

    /**
     * 未知异常
     */
    UNKNOWN,

    /**
     * 删除中
     */
    DELETING,

    /**
     * 已删除
     */
    DELETED,

    /**
     * 完成态（运行失败）
     */
    FAILED,

    /**
     * 已过期
     */
    EXPIRED,

    /**
     * 尚未实现
     */
    NOT_IMPLEMENTED
}
