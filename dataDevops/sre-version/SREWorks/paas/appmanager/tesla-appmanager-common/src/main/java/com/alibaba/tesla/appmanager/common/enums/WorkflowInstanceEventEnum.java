package com.alibaba.tesla.appmanager.common.enums;

/**
 * Workflow 实例事件 Enum
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum WorkflowInstanceEventEnum {

    /**
     * 开始事件
     */
    START,

    /**
     * 预处理完成
     */
    PREPROCESS_FINISHED,

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
     * 终止
     */
    OP_TERMINATE,

    /**
     * 重试
     */
    OP_RETRY
}
