package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 实时 Trait 实例表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RtTraitInstanceDO {
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
     * Trait 实例 ID
     */
    private String traitInstanceId;

    /**
     * 当前 Trait 实例归属的组件实例 ID
     */
    private String componentInstanceId;

    /**
     * 当前 Trait 实例归属的应用实例 ID
     */
    private String appInstanceId;

    /**
     * Trait 唯一名称 (含版本)
     */
    private String traitName;

    /**
     * 状态
     */
    private String status;

    /**
     * 当前状态详情 (Yaml Array)
     */
    private String conditions;

    /**
     * 锁版本
     */
    private Integer lockVersion;
}