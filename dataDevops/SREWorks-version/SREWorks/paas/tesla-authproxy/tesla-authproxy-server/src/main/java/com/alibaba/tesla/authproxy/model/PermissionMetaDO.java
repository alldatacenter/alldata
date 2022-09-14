package com.alibaba.tesla.authproxy.model;

import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.Size;
import java.io.Serializable;
import java.util.Date;

/**
 * Tesla服务权限元数据
 * @author tandong.td@alibaba-inc.com
 */
@Data
public class PermissionMetaDO implements Serializable {

    private long id;

    /**
     * Tesla服务标识，表示该权限元数据属于哪个Tesla服务
     */
    @NotEmpty(message = "serviceCode is required")
    @Size(min = 1, max = 50, message = "the length of serviceCode must between 1 and 50")
    private String serviceCode;

    /**
     * Tesla服务名称
     */
    @NotEmpty(message = "serviceName is required")
    @Size(min = 1, max = 50, message = "the length of serviceName must between 1 and 50")
    private String serviceName;

    /**
     * 权限标识
     */
    @NotEmpty(message = "permissionCode is required")
    @Size(min = 1, max = 100, message = "the length of permissionCode must between 1 and 100")
    private String permissionCode;

    /**
     * 权限类型
     * 0-接口权限
     * 1-菜单权限
     * 2-功能操作权限，如操作按钮的权限
     * 3-所有权限
     */
    private int permissionType;

    /**
     * 权限名称
     */
    @NotEmpty(message = "permissionName is required")
    @Size(min = 1, max = 100, message = "the length of permissionName must between 1 and 100")
    private String permissionName;

    /**
     * 是否生效
     */
    private int isEnable;

    /**
     * 权限申请链接地址
     */
    private String applyUrl;

    /**
     * 备注信息
     */
    @NotEmpty(message = "memo is required")
    @Size(min = 1, max = 100, message = "the length of memo must between 1 and 100")
    private String memo;

    private Date gmtCreate;

    private Date gmtModified;


    /**
     * 扩展字段：授权ID，为空表示未授权给任何app
     */
    private Long grantResId;
}