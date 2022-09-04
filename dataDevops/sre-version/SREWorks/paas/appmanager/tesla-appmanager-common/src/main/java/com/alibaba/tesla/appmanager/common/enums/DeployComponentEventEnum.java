package com.alibaba.tesla.appmanager.common.enums;

/**
 * Component 部署单事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum DeployComponentEventEnum {

    /**
     * 启动
     */
    START,

    /**
     * 资源满足需求
     */
    RESOURCE_SUFFICIENT,

    /**
     * 资源不满足需求
     */
    RESOURCE_INSUFFICIENT,

    /**
     * 处理完毕
     */
    PROCESS_FINISHED,

    /**
     * 流程运行成功
     */
    FLOW_SUCCEED,

    /**
     * 流程运行失败
     */
    FLOW_FAILED,

    /**
     * 操作事件: 重试
     */
    OP_RETRY,

    /**
     * 操作事件: 终止
     */
    OP_TERMINATE,

    /**
     * 触发 Component 部署单状态更新事件
     */
    TRIGGER_UPDATE;
}
