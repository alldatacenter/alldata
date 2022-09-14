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
 * 用户与角色关联表
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Table(name = "ta_user_role_rel")
@Entity
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserRoleRelDO implements Serializable {

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
     * 用户 ID
     */
    @Column(length = 64, nullable = false)
    private String userId;

    /**
     * 角色 ID
     */
    @Column(length = 64, nullable = false)
    private String roleId;

    /**
     * 创建时间
     */
    @CreatedDate
    @Column
    private ZonedDateTime gmtCreate;

    /**
     * 最后修改时间
     */
    @LastModifiedDate
    @Column
    private ZonedDateTime gmtModified;

    /**
     * 角色名称
     */
    @Transient
    private String roleName;

    /**
     * 角色描述
     */
    @Transient
    private String roleDescription;
}
