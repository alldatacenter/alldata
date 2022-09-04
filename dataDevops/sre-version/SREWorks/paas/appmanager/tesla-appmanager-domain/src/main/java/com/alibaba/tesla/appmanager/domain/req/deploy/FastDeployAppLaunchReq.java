package com.alibaba.tesla.appmanager.domain.req.deploy;

import com.alibaba.tesla.appmanager.domain.req.converter.AddParametersToLaunchReq;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 快速发起一次 AppPackage 的部署单请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FastDeployAppLaunchReq {

    /**
     * 应用包 ID
     */
    private Long appPackageId;

    /**
     * Yaml文件
     */
    private String swapp;

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
     * App Instance Name
     */
    private String appInstanceName;

    /**
     * 需要附加的全局参数列表
     */
    private List<AddParametersToLaunchReq.ParameterValue> parameterValues = new ArrayList<>();
}
