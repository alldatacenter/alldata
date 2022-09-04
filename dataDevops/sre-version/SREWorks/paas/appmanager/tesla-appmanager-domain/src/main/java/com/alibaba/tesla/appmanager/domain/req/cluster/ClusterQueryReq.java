package com.alibaba.tesla.appmanager.domain.req.cluster;

import com.alibaba.tesla.appmanager.common.BaseRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Cluster 查询请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterQueryReq extends BaseRequest {

    private static final long serialVersionUID = -1872143932301240289L;

    /**
     * Cluster ID
     */
    private String clusterId;

    /**
     * Cluster 名称
     */
    private String clusterName;

    /**
     * Cluster 类型
     */
    private String clusterType;
}
