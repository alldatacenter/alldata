package com.alibaba.tesla.appmanager.common.enums;

/**
 * 部署单 - APP 层面事件
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public enum DeployAppEventEnum {

    /**
     * 启动
     */
    START,

    /**
     * 组件检测，全部存在
     */
    COMPONENTS_EXISTS,

    /**
     * 组件检测，存在未生成包体的组件
     */
    COMPONENTS_NOT_FOUND,

    /**
     * 组件包体制作，已全部完成
     */
    COMPONENTS_FINISHED,

    /**
     * 组件包体制作，存在失败
     */
    COMPONENTS_FAILED,

    /**
     * 组件包制作任务已创建
     */
    COMPONENTS_TASK_CREATED,

    /**
     * 当前基线对应的应用包备份已存在
     */
    APP_PACKAGE_EXIST,

    /**
     * 当前基线对应的应用包备份不存在
     */
    APP_PACKAGE_NOT_FOUND,

    /**
     * 备份成功
     */
    BACKUP_SUCCEED,

    /**
     * 备份失败
     */
    BACKUP_FAILED,

    /**
     * 处理完毕
     */
    PROCESS_FINISHED,

    /**
     * 全部 Component 部署单成功
     */
    ALL_SUCCEED,

    /**
     * 存在 Component 部署单失败
     */
    PARTIAL_FAILED,

    /**
     * 触发回滚
     */
    OP_ROLLBACK,

    /**
     * 终止当前部署单
     */
    OP_TERMINATE,

    /**
     * 重试当前部署单
     */
    OP_RETRY,

    /**
     * 触发 App 部署单状态更新事件
     */
    TRIGGER_UPDATE;
}
