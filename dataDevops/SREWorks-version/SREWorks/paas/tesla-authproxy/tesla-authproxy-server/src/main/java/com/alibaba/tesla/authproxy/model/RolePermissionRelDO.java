package com.alibaba.tesla.authproxy.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.ZonedDateTime;

/**
 * 角色与权限关联表
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Table(name = "ta_role_permission_rel")
@Entity
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RolePermissionRelDO implements Serializable {

    /**
     * 主键 ID
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * 租户 ID
     */
    @Column(length = 32, nullable = false)
    private String tenantId;

    /**
     * 角色 ID
     */
    @Column(length = 64, nullable = false)
    private String roleId;

    /**
     * 资源路径
     */
    @Column(length = 256, nullable = false)
    private String resourcePath;

    /**
     * 服务代码
     */
    @Column(length = 32, nullable = false)
    private String serviceCode;

    /**
     * 创建日期
     */
    @CreatedDate
    @Column
    private ZonedDateTime gmtCreate;

    /**
     * 最后修改日期
     */
    @LastModifiedDate
    @Column
    private ZonedDateTime gmtModified;
}
