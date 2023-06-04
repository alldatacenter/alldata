package org.dromara.cloudeon.entity;

import lombok.Data;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 服务实例配置表
 */
@Entity
@Table(name = "ce_service_instance_config")
@Data
public class ServiceInstanceConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * 主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    @GenericGenerator(name = "native", strategy = "native")
    private Integer id;

    /**
     * 服务实例id
     */
    private Integer serviceInstanceId;

    /**
     * 配置项
     */
    private String name;


    private Integer nodeId;

    /**
     * 值
     */
    private String value;

    /**
     * 所属配置文件
     */
    private String confFile;
    private String tag;

    /**
     * 是否属于自定义配置
     */
    private boolean isCustomConf;

    /**
     * 框架推荐值
     */
    private String recommendedValue;


    /**
     * 更新时间
     */
    private Date updateTime;
    /**
     * 创建时间
     */
    private Date createTime;


    private Integer userId;



}