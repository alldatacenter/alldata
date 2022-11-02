package com.alibaba.tesla.appmanager.domain.req.destroy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 销毁组件实例请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DestroyComponentInstanceReq implements Serializable {

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 应用实例 ID
     */
    private String appInstanceId;

    /**
     * 组件实例 ID
     */
    private String componentInstanceId;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件名称
     */
    private String componentName;

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
}
