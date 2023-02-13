package com.alibaba.tesla.appmanager.domain.req.cluster;

import com.alibaba.fastjson.JSONObject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * Cluster 更新请求
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClusterUpdateReq implements Serializable {

    private static final long serialVersionUID = 2100141559164672443L;

    /**
     * Cluster 名称
     */
    private String clusterName;

    /**
     * Cluster 类型
     */
    private String clusterType;

    /**
     * Cluster 配置
     */
    private JSONObject clusterConfig;

    /**
     * 是否为主集群标识
     */
    private Boolean masterFlag;
}
