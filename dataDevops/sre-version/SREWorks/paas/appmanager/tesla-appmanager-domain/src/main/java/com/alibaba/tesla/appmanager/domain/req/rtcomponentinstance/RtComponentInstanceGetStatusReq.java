package com.alibaba.tesla.appmanager.domain.req.rtcomponentinstance;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 组件实例获取状态请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RtComponentInstanceGetStatusReq {

    private String clusterId;

    private String namespaceId;

    private String stageId;

    private String appId;

    private String componentType;

    private String componentName;

    private String version;
}
