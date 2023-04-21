package org.dromara.cloudeon.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;

/**
 * 集群服务角色对应web ui表
 */

@Entity
@Table(name = "ce_service_role_instance_webuis")
@Data
public class ServiceRoleInstanceWebuisEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;
    /**
     * 服务角色id
     */
    private Integer serviceRoleInstanceId;
    /**
     * URL地址(hostname)
     */
    private String webHostUrl;

    /**
     * URL地址(ip)
     */
    private String webIpUrl;
    /**
     * 服务实例id
     */
    private Integer serviceInstanceId;

    /**
     * 访问页面的名称
     */
    private String name;

}
