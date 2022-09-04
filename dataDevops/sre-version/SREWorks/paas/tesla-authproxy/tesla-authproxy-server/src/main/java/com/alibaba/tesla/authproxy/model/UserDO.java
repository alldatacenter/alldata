package com.alibaba.tesla.authproxy.model;

import com.alibaba.tesla.authproxy.AuthProperties;
import com.alibaba.tesla.authproxy.Constants;
import com.alibaba.tesla.authproxy.lib.exceptions.AuthProxyThirdPartyError;
import com.alibaba.tesla.authproxy.outbound.oam.OamClient;
import com.alibaba.tesla.authproxy.util.ApplicationContextUtil;

import com.aliyuncs.exceptions.ClientException;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.DynamicInsert;
import org.hibernate.annotations.DynamicUpdate;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import javax.persistence.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * 登陆用户信息持久对象
 *
 * @author tandong.td@alibaba-inc.com
 * @author yaoxing.gyx@alibaba-inc.com
 */
@Table(name = "ta_user")
@Entity
@EntityListeners(AuditingEntityListener.class)
@DynamicInsert
@DynamicUpdate
@Data
@Slf4j
public class UserDO implements UserDetails, Serializable {

    private static final String LOG_PRE = "[" + UserDO.class.getSimpleName() + "] ";

    /**
     * 用户 ID (数据库)
     */
    @Id
    @GeneratedValue
    private Long id;

    /**
     * 租户 ID
     */
    @Column(length = 32)
    private String tenantId;

    /**
     * 租户下唯一标识符
     */
    @Column(length = 64)
    private String userId;

    /**
     * 内外唯一用户定位名称 (内部使用) 注意：对内使用邮箱时自动去除对应邮箱后缀
     */
    @Column(length = 128)
    private String loginName;

    /**
     * 登录密码
     */
    @Column(length = 128)
    private String loginPwd;

    /**
     * 第一版鉴权使用，针对用户的 secret key 进行签名计算, 同对内 auth-proxy 计算方式
     */
    @Column(length = 64)
    private String secretKey = "";

    /**
     * 第二版鉴权使用，对内用户定位信息
     */
    @Column(length = 45)
    private String bid;

    @Column(length = 128)
    private String empId;

    @Column
    private Long bucId;

    /**
     * 部门 ID
     */
    @Column(length = 32)
    private String depId;

    /**
     * 第二版鉴权使用，对外用户定位信息
     */
    @Column(length = 45)
    private String aliyunPk;

    @Column(length = 45)
    private String accessKeyId = "";

    @Column(length = 45)
    private String accessKeySecret = "";

    /**
     * 用户附加信息
     */
    @Column(length = 45)
    private String nickName;

    @Column(length = 128)
    private String email;

    @Column(length = 45)
    private String phone;

    @Column(length = 45)
    private String dingding;

    @Column(length = 64)
    private String aliww;

    @Column(length = 45)
    private String memo;

    @Column(length = 1024)
    private String avatar;

    /**
     * 用户自身登陆属性及当前状态
     */
    @Column
    @ColumnDefault("0")
    private Byte isFirstLogin = 0;

    @Column
    @ColumnDefault("0")
    private Byte isLocked = 0;

    @Column
    private Integer status = 0;

    private String lang = "";

    @Column
    @ColumnDefault("0")
    private Byte isImmutable = 0;

    @Column
    private Date lastLoginTime;

    /**
     * 创建日期
     */
    @CreatedDate
    @Column
    private Date gmtCreate;

    /**
     * 最后修改日期
     */
    @LastModifiedDate
    @Column
    private Date gmtModified;

    /**
     * 角色列表
     */
    @Transient
    private List<UserRoleRelDO> roles;

    /**
     * 扩展信息字典
     */
    @Transient
    private UserExtDO ext = new UserExtDO();

    public void setExt(UserExtDO ext) {
        this.ext = ext;
        this.ext.setAvatar(avatar);
    }

    /**
     * 返回用户被授权的角色列表
     *
     * @return 角色列表
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        //AuthProperties authProperties = (AuthProperties)ApplicationContextUtil.getContext().getBean("authProperties");
        //if (!Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())
        //    && !Constants.ENVIRONMENT_OXS.equals(authProperties.getEnvironment())
        //    && !Constants.ENVIRONMENT_STANDALONE.equals(authProperties.getEnvironment())
        //    && !Constants.ENVIRONMENT_PAAS.equals(authProperties.getEnvironment())
        //) {
        //    OamClient oamClient = (OamClient)ApplicationContextUtil.getContext().getBean("oamClient");
        //    List<ListRoleByOperatorResponse.OamRole> roles;
        //    try {
        //        roles = oamClient.listRoleByOperator(aliyunPk, bid, aliyunPk);
        //    } catch (AuthProxyThirdPartyError | ClientException e) {
        //        log.warn(LOG_PRE + "List oam roles for user(loginName={}, aliyunPk={}, bid={}) failed, exception={}",
        //            loginName, aliyunPk, bid, ExceptionUtils.getStackTrace(e));
        //        return new ArrayList<>();
        //    }
        //    List<GrantedAuthority> result = new ArrayList<>();
        //    for (ListRoleByOperatorResponse.OamRole role : roles) {
        //        result.add(new SimpleGrantedAuthority(role.getRoleName()));
        //    }
        //    return result;
        //} else {
        //    return new ArrayList<>();
        //}
        return new ArrayList<>();
    }

    /**
     * 获取当前用户的 Spring 中对应的 密码
     * <p>
     * 对内构造规则: $loginName/$secretKey
     * <p>
     * 对外构造规则：$aliyunPk/$accessKeyId/$accessKeySecret
     *
     * @return 构造出来的用户密码
     */
    @JsonIgnore
    @Override
    public String getPassword() {
        AuthProperties authProperties = (AuthProperties)ApplicationContextUtil.getContext().getBean("authProperties");
        if (!Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
            return accessKeySecret;
        } else {
            return secretKey;
        }
    }

    @JsonIgnore
    @Override
    public String getUsername() {
        AuthProperties authProperties = (AuthProperties)ApplicationContextUtil.getContext().getBean("authProperties");
        if (!Constants.ENVIRONMENT_INTERNAL.equals(authProperties.getEnvironment())) {
            return accessKeyId;
        } else {
            return loginName;
        }
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @JsonIgnore
    @Override
    public boolean isEnabled() {
        return true;
    }
}