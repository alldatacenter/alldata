package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import lombok.Data;

/**
 * 集群服务角色实例配置表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@TableName("t_ddh_cluster_service_instance_config")
@Data
public class ClusterServiceInstanceConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主机
     */
    @TableId
    private Integer id;
    /**
     * 服务角色实例id
     */
    private Integer serviceId;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 配置json
     */
    private String configJson;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 配置json md5
     */
    private String configJsonMd5;
    /**
     * 配置json版本
     */
    private Integer configVersion;
    /**
     *
     */
    private Integer clusterId;

    private String configFileJson;

    private String configFileJsonMd5;


}
