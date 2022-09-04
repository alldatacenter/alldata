package com.alibaba.tesla.authproxy.api.model;

import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 验权请求参数结构体
 *
 * @author tandong.td@alibaba-inc.com
 */
public class CheckPermissionRequest implements Serializable {

    /**
     * 对接权代服务的应用ID
     */
    @NotEmpty(message = "appId can't be empty")
    String appId;

    /**
     * 进行权限验证的用户工号
     */
    @NotEmpty(message = "userId can't be empty")
    String userId;

    /**
     * 应用在权代服务中的权限名称
     */
    @NotEmpty(message = "permissionName can't be empty")
    String permissionName;

    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }
}
