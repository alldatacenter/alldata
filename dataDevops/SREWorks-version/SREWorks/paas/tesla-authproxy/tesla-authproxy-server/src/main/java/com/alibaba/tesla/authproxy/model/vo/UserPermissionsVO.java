package com.alibaba.tesla.authproxy.model.vo;

import java.io.Serializable;

/**
 * 权限资源验证结果集合
 * 返回了有权限或没权限的集合结果数据
 */
public class UserPermissionsVO implements Serializable {

    /**
     * 资源path
     */
    String reqPath;

    /**
     * 中文名称
     */
    String name;

    /**
     * acl权限名称
     */
    String permissionName;

    /**
     * 该用户是否有访问权限
     */
    boolean accessible = false;

    /**
     * 权限申请链接地址
     */
    String applyLink;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReqPath() {
        return reqPath;
    }

    public void setReqPath(String reqPath) {
        this.reqPath = reqPath;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public boolean isAccessible() {
        return accessible;
    }

    public void setAccessible(boolean accessible) {
        this.accessible = accessible;
    }

    public String getApplyLink() {
        return applyLink;
    }

    public void setApplyLink(String applyLink) {
        this.applyLink = applyLink;
    }
}
