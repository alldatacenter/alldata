package com.datasophon.dao.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.io.Serializable;
import java.util.Date;

import com.datasophon.dao.enums.NeedRestart;
import com.datasophon.dao.enums.RoleType;
import com.datasophon.dao.enums.ServiceRoleState;
import lombok.Data;

/**
 * 集群服务角色实例表
 *
 * @author dygao2
 * @email gaodayu2022@163.com
 * @date 2022-04-24 16:25:17
 */
@TableName("t_ddh_cluster_service_role_instance")
@Data
public class ClusterServiceRoleInstanceEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @TableId
    private Integer id;
    /**
     * 服务角色名称
     */
    private String serviceRoleName;
    /**
     * 主机
     */
    private String hostname;
    /**
     * 服务角色状态 1:正在运行2：存在告警3：存在异常4：需要重启
     */
    private ServiceRoleState serviceRoleState;

    @TableField(exist = false)
    private Integer serviceRoleStateCode;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 服务id
     */
    private Integer serviceId;
    /**
     * 角色类型 1:master2:worker3:client
     */
    private RoleType roleType;
    /**
     * 集群id
     */
    private Integer clusterId;
    /**
     * 服务名称
     */
    private String serviceName;

    private Integer roleGroupId;

    private NeedRestart needRestart;

    @TableField(exist = false)
    private String roleGroupName;

}
