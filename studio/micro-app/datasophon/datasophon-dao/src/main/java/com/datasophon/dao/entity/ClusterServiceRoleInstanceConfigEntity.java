package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.util.Date;

/**
 * 集群服务角色实例配置表
 *
 * @author gaodayu
 * @email gaodayu2022@163.com
 * @date 2022-03-15 17:21:05
 */
@Data
@TableName("t_ddh_cluster_service_role_instance_config")
public class ClusterServiceRoleInstanceConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主机
     */
    @TableId
    private Integer id;
    /**
     * 服务角色实例id
     */
    private Integer service_role_instance_id;
    /**
     * 创建时间
     */
    private Date create_time;
    /**
     * 配置json
     */
    private String config_json;
    /**
     * 更新时间
     */
    private Date update_time;
    /**
     * 配置json md5
     */
    private String config_json_md5;
    /**
     * 配置json版本
     */
    private String config_json_version;

}
