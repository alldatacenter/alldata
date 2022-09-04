package com.alibaba.tesla.appmanager.domain.req.rtcomponentinstance;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * 实时组件实例 Query 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class RtComponentInstanceQueryReq extends BaseRequest {

    /**
     * 应用实例 ID
     */
    private String appInstanceId;

    /**
     * 应用 ID
     */
    private String appId;

    /**
     * 应用 ID 以什么开始 (可通过 ! 前置反转)
     */
    private String appIdStartsWith;

    /**
     * Cluster ID
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
     * 状态
     */
    private String status;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 组件名称以什么开始 (可通过 ! 前置反转)
     */
    private String componentNameStartsWith;

    /**
     * 应用选项过滤 Key
     */
    private String optionKey;

    /**
     * 应用选项过滤 Value
     */
    private String optionValue;

    /**
     * 是否反转查询
     */
    private Boolean reverse = true;
}
