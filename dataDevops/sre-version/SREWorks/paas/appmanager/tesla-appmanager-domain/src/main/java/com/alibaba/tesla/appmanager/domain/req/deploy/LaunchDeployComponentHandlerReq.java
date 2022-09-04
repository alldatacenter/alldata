package com.alibaba.tesla.appmanager.domain.req.deploy;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 部署组件请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LaunchDeployComponentHandlerReq implements Serializable {

    /**
     * 部署 App ID
     */
    private Long deployAppId;

    /**
     * 部署 Component ID
     */
    private Long deployComponentId;

    /**
     * 标识符
     */
    private String identifier;

    /**
     * 应用 ID
     */
    private String appId;

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
     * 组件包 URL
     */
    private String componentPackageUrl;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 组件 Schema 定义
     */
    private ComponentSchema componentSchema;

    /**
     * 组件部署选项 (ApplicationConfiguration 中的配置)
     */
    private DeployAppSchema.SpecComponent componentOptions;

    /**
     * Owner Reference
     */
    private String ownerReference;

    /**
     * 是否在本地集群中
     */
    private boolean inLocalCluster;
}
