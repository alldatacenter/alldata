package org.dromara.cloudeon.entity;

import org.dromara.cloudeon.enums.ServiceRoleState;
import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 集群服务角色实例表
 *
 */
@Entity
@Table(name = "ce_service_role_instance")
@Data
public class ServiceRoleInstanceEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;
    /**
     * 服务角色名称
     */
    private String serviceRoleName;
    /**
     * 主机id
     */
    private Integer nodeId;
    /**
     * 服务角色状态
     */
    @Convert(converter = RoleStateConverter.class)
    private ServiceRoleState serviceRoleState;


    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建时间
     */
    private Date createTime;
    /**
     * 框架服务角色id
     */
    private Integer stackServiceRoleId;


    /**
     * 角色类型
     */
    private String roleType;
    /**
     * 集群id
     */
    private Integer clusterId;
    /**
     * 所属服务实例id
     */
    private Integer serviceInstanceId;


    private boolean needRestart;

    private boolean isDecommission;


    public ServiceRoleInstanceEntity() {

    }
}