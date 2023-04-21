package org.dromara.cloudeon.controller.response;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群信息表
 *
 */
@Data
public class ClusterInfoVO implements Serializable {
    private static final long serialVersionUID = 1L;


    private Integer id;
    /**
     * 创建人
     */
    private String createBy;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 集群名称
     */
    private String clusterName;
    /**
     * 集群编码
     */
    private String clusterCode;
    /**
     * 集群框架
     */
    private Integer stackId;

    private String kubeConfig;

    /**
     * 绑定节点数
     */
    private Integer nodeCnt;

    /**
     * 服务数
     */
    private Integer serviceCnt;




}
