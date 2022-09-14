package com.alibaba.tesla.appmanager.server.repository.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群表
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClusterDO implements Serializable {
    /**
     * ID
     */
    private Long id;

    /**
     * 创建时间
     */
    private Date gmtCreate;

    /**
     * 最后修改时间
     */
    private Date gmtModified;

    /**
     * 集群标识
     */
    private String clusterId;

    /**
     * 集群名称
     */
    private String clusterName;

    /**
     * 集群类型
     */
    private String clusterType;

    /**
     * 集群配置
     */
    private String clusterConfig;

    /**
     * Master 标记
     */
    private Boolean masterFlag;

    private static final long serialVersionUID = 1L;
}