package com.alibaba.tesla.appmanager.domain.req.deploy;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.tesla.appmanager.domain.schema.ComponentSchema;
import com.alibaba.tesla.appmanager.domain.schema.DeployAppSchema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Map;

/**
 * 获取部署组件结果的查询对象
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetDeployComponentHandlerReq implements Serializable {

    /**
     * 部署 App ID
     */
    private Long deployAppId;

    /**
     * 部署 Component ID
     */
    private Long deployComponentId;

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
     * 组件类型
     */
    private String componentType;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 全局属性字典
     */
    private Map<String, String> attrMap;
}
