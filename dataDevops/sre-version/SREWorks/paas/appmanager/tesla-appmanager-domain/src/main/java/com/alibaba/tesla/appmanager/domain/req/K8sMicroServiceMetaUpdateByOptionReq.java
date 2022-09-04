package com.alibaba.tesla.appmanager.domain.req;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * K8S 微服务更新请求 (通过 options 对象)
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class K8sMicroServiceMetaUpdateByOptionReq {

    /**
     * 应用标示
     */
    private String appId;

    /**
     * Namespace ID
     */
    private String namespaceId;

    /**
     * Stage ID
     */
    private String stageId;

    /**
     * 配置项 JSONObject (componentType/componentName/options...)
     */
    private JSONObject body;

    /**
     * 归属产品 ID
     */
    private String productId;

    /**
     * 归属发布版本 ID
     */
    private String releaseId;
}
