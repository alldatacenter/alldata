package com.alibaba.tesla.appmanager.domain.req.kubectl;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Kubectl Delete StatefulSet 请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KubectlDeleteStatefulSetReq implements Serializable {

    /**
     * 目标集群
     */
    private String clusterId;

    /**
     * 目标 Namespace
     */
    private String namespaceId;

    /**
     * StatefulSet Name
     */
    private String statefulSetName;
}
