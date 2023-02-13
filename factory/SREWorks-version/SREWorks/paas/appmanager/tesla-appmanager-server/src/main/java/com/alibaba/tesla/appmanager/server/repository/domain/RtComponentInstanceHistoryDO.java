package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 实时组件实例表_历史
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RtComponentInstanceHistoryDO {
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 修改时间
     */
    private Date gmtModified;

    /**
     * 组件实例 ID
     */
    private String componentInstanceId;

    /**
     * 当前组件归属的应用实例 ID
     */
    private String appInstanceId;

    /**
     * 状态
     */
    private String status;

    /**
     * 当前状态详情 (JSON Array)
     */
    private String conditions;

    /**
     * 组件实例版本号
     */
    private String version;
}