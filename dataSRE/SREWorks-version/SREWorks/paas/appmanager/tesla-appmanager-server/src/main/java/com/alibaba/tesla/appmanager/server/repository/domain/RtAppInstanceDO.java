package com.alibaba.tesla.appmanager.server.repository.domain;

import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 实时应用实例表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RtAppInstanceDO {
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
     * 应用实例 ID
     */
    private String appInstanceId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * Cluster ID
     */
    private String clusterId;

    /**
     * 应用实例版本号 (最近一次应用包部署)
     */
    private String version;

    /**
     * 状态
     */
    private String status;

    /**
     * 锁版本
     */
    private Integer lockVersion;

    /**
     * 是否可访问
     */
    private Boolean visit;

    /**
     * 是否可升级
     */
    private Boolean upgrade;

    /**
     * 应用实例可升级最新版本号
     */
    private String latestVersion;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 应用实例可读名称
     */
    private String appInstanceName;

    /**
     * Owner Reference
     */
    private String ownerReference;

    /**
     * Parent Owner Reference
     */
    private String parentOwnerReference;
}