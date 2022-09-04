package com.alibaba.tesla.appmanager.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * App 部署单详细信息
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeployAppDTO {

    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 可读创建时间
     */
    private String readableGmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 可读最后修改时间
     */
    private String readableGmtModified;

    /**
     * 当前部署单使用的应用包 ID
     */
    private Long appPackageId;

    /**
     * 应用包版本
     */
    private String packageVersion;

    /**
     * 应用包版本号（简化）
     */
    private String simplePackageVersion;

    /**
     * 应用唯一标识
     */
    private String appId;

    /**
     * 部署目标 Cluster
     */
    private String clusterId;

    /**
     * 部署目标 Namespace
     */
    private String namespaceId;

    /**
     * 部署目标 Stage
     */
    private String stageId;

    /**
     * 部署开始时间
     */
    private Date gmtStart;

    /**
     * 可读部署开始时间
     */
    private String readableGmtStart;

    /**
     * 部署结束时间
     */
    private Date gmtEnd;

    /**
     * 可读部署结束时间
     */
    private String readableGmtEnd;

    /**
     * 执行消耗时间
     */
    private String cost;

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
     * 部署过程中的 Application Configuration 配置 (YAML)
     */
    private String deployAppConfiguration;

    /**
     * 部署流程 ID
     */
    private Long deployProcessId;

    /**
     * 部署配置 SHA256
     */
    private String configSha256;

    /**
     * 包含的 Component 部署单详情
     */
    private List<DeployComponentDTO> deployComponents = new ArrayList<>();
}
