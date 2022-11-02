package com.alibaba.tesla.appmanager.common.enums;

/**
 * Workflow 实例事件 Enum
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum WorkflowTaskEventEnum {

    /**
     * 开始事件
     */
    START,

    /**
     * 暂停
     */
    PAUSE,

    /**
     * 恢复
     */
    RESUME,

    /**
     * 处理完成
     */
    PROCESS_FINISHED,

    /**
     * 处理失败
     */
    PROCESS_FAILED,

    /**
     * 处理过程中产生未知错误
     */
    PROCESS_UNKNOWN_ERROR,

    /**
     * 处理过程中进入休眠状态
     */
    PROCESS_SUSPEND,

    /**
     * 等待过程成功
     */
    WAITING_FINISHED,

    /**
     * 等待过程失败
     */
    WAITING_FAILED,

    /**
     * 等待过程出现未知错误
     */
    WAITING_UNKNOWN_ERROR,

    /**
     * 终止
     */
    OP_TERMINATE,

    /**
     * 重试
     */
    OP_RETRY,

    /**
     * 重试当前步骤
     */
    TRIGGER_UPDATE,
}
