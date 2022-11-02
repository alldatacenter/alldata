package com.alibaba.tesla.appmanager.server.service.componentpackage.instance.constant;

/**
 * @EnumName:PodStatusPhaseEnum
 * @Author:dyj
 * @DATE: 2021-02-05
 * @Description:
 **/
public enum PodStatusPhaseEnum {
    /**
     * 等待
     */
    Pending,
    /**
     * 有一个 container 处于running。
     */
    Running,
    /**
     * 所有 container 都成功终止
     */
    Succeeded,
    /**
     * 所有容器都终止，且至少一个 container 处于 fail
     */
    Failed,
    /**
     * 无法取得 pod 状态
     */
    Unknown;

}
