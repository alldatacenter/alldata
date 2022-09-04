package com.alibaba.tesla.appmanager.common.enums;

/**
 * 产品发布版本任务状态 Enum
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum ProductReleaseTaskStatusEnum {

    /**
     * 跳过
     */
    SKIP,

    /**
     * 等待中
     */
    PENDING,

    /**
     * 运行中
     */
    RUNNING,

    /**
     * 已成功
     */
    SUCCESS,

    /**
     * 失败
     */
    FAILURE,

    /**
     * 异常
     */
    EXCEPTION;
}
