package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * 部署工单 - AppPackage 表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DeployAppDO {
    private static final long serialVersionUID = 1L;
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 当前部署单使用的应用包 ID
     */
    private Long appPackageId;

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 部署开始时间
     */
    private Date gmtStart;

    /**
     * 部署结束时间
     */
    private Date gmtEnd;

    /**
     * 状态
     */
    private String deployStatus;

    /**
     * 错误信息
     */
    private String deployErrorMessage;

    /**
     * 部署工单发起人
     */
    private String deployCreator;

    /**
     * 部署流程 ID
     */
    private Long deployProcessId;

    /**
     * 应用包版本
     */
    private String packageVersion;

    /**
     * VERSION
     */
    private Integer version;

    /**
     * 集群 ID
     */
    private String clusterId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * Configuration SHA256
     */
    private String configSha256;

    /**
     * 返回部署过程消耗的时间
     *
     * @return 字符串
     */
    public String costTime() {
        if (gmtCreate == null || gmtModified == null) {
            return "unknown";
        }
        return String.format("%d", gmtModified.getTime() - gmtCreate.getTime());
    }
}