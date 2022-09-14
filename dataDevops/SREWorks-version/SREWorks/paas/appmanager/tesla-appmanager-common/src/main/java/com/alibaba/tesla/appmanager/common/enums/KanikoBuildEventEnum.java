package com.alibaba.tesla.appmanager.common.enums;

/**
 * @EnumName:KanikoBuildEventEnum
 * @Author:dyj
 * @DATE: 2021-07-14
 * @Description:
 **/
public enum KanikoBuildEventEnum {
    /**
     * 所有 container 都成功终止
     */
    SUCCEED,
    /**
     * 所有容器都终止，且至少一个 container 处于 fail
     */
    FAILED,

    /**
     * pod 删除
     */
    DELETE,
}
