package org.dromara.cloudeon.entity;

import org.dromara.cloudeon.enums.ServiceState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 服务实例表
 */
@Entity
@Table(name = "ce_service_instance")
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ServiceInstanceEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;




    /**
     * 集群id
     */
    private Integer clusterId;

    /**
     * 服务名称
     */
    private String serviceName;

    private String label;
    /**
     * 服务状态
     */
    @Convert(converter = ServiceStateConverter.class)
    private ServiceState serviceState;

    /**
     * 是否需要重启
     */
    private Boolean needRestart;
    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 该服务实例所依赖的服务实例id
     */
    private String dependenceServiceInstanceIds;
    /**
     * 框架服务id
     */
    private Integer stackServiceId;

    /**
     * 相同框架服务的内部实例序号
     */
    private Integer instanceSequence;

    /**
     * 是否开启Kerberos
     */
    private Boolean enableKerberos;

    private String persistencePaths;



}