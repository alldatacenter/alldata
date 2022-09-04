package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 实时 Trait 实例表_历史
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RtTraitInstanceHistoryDO {
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
     * 状态
     */
    private String status;

    /**
     * 当前状态详情 (JSOKN Array)
     */
    private String conditions;

    /**
     * Trait 实例版本号
     */
    private String version;
}