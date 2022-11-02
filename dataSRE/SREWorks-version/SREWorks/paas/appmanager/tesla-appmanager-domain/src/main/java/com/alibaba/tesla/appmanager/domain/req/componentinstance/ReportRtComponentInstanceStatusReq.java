package com.alibaba.tesla.appmanager.domain.req.componentinstance;

import com.alibaba.tesla.appmanager.domain.core.InstanceCondition;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

/**
 * 上报组件实例状态请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportRtComponentInstanceStatusReq implements Serializable {

    /**
     * 组件实例 ID
     */
    private String componentInstanceId;

    /**
     * 应用实例名称
     */
    private String appInstanceName;

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
     * 应用 ID
     */
    private String appId;

    /**
     * 组件类型
     */
    private String componentType;

    /**
     * 组件名称
     */
    private String componentName;

    /**
     * 版本
     */
    private String version;

    /**
     * 状态
     */
    private String status;

    /**
     * 状态详情
     */
    private List<InstanceCondition> conditions;
}
