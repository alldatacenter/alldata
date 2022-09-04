package com.alibaba.tesla.appmanager.common.enums;

/**
 * Trait 实例状态
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum TraitInstanceStatusEnum {

    /**
     * 进入等待状态
     */
    PENDING,

    /**
     * 正在运行中
     */
    RUNNING,

    /**
     * 运行完成，已正常退出
     */
    COMPLETED,

    /**
     * 运行完成，异常退出
     */
    FAILED,

    /**
     * 监听状态中
     */
    WATCHING;
}
