package com.alibaba.tesla.appmanager.domain.req.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 获取指定 appPackage 下的 Launch Yaml 文件
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppPackageGetLaunchYamlReq {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 应用包 ID (appPackageId / appPackageVersion 二选一)
     */
    private long appPackageId;

    /**
     * 应用包版本 (appPackageId / appPackageVersion 二选一)
     */
    private String appPackageVersion;

    /**
     * 应用实例名称
     */
    private String appInstanceName;

    /**
     * 单元 ID
     */
    private String unit;

    /**
     * 集群 ID
     */
    private String clusterId;

    /**
     * NamespaceID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 是否组件包配置优先 (当 true 时, 不使用系统存储的组件 deploy configurations)
     */
    private boolean componentPackageConfigurationFirst = false;
}
