package com.alibaba.tesla.appmanager.domain.req.apppackage;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ApplicationConfiguration 配置创建请求对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationConfigurationGenerateReq {

    /**
     * API 版本
     */
    private String apiVersion;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 应用包 ID
     */
    private long appPackageId;

    /**
     * 应用实例名称, 可选
     */
    private String appInstanceName;

    /**
     * 单元 ID，可选
     */
    private String unitId;

    /**
     * 集群 ID, 可选
     */
    private String clusterId;

    /**
     * Namespace ID, 可选
     */
    private String namespaceId;

    /**
     * Stage ID, 可选
     */
    private String stageId;

    /**
     * Type ID 列表，可选
     */
    private List<String> typeIds;

    /**
     * 是否组件包配置优先 (当 true 时, 不使用系统存储的组件 deploy configurations)
     */
    private boolean componentPackageConfigurationFirst = false;

    /**
     * Isolate Namespace ID
     */
    private String isolateNamespaceId;

    /**
     * Isolate Stage ID
     */
    private String isolateStageId;
}
