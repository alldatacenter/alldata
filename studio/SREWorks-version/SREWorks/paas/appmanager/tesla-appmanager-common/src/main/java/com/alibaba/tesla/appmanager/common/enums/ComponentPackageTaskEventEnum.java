package com.alibaba.tesla.appmanager.common.enums;

/**
 * Component 部署单事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum ComponentPackageTaskEventEnum {

    /**
     * 启动
     */
    START,

    /**
     * 运行成功
     */
    SUCCEED,

    /**
     * 跳过
     */
    SKIPPED,

    /**
     * 运行失败
     */
    FAILED,

    /**
     * 人工操作: 重试
     */
    OP_RETRY;
}
