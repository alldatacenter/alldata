package com.alibaba.tesla.authproxy.model.vo;

import java.util.List;

/**
 * <p>Description:  添加权限数据JSON对应的VO对象 <／p>
 * <p>Copyright: alibaba (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017/5/9 下午1:47
 */
public class AddPermissionVO {

    /**
     * 应用标识
     */
    private String appId;

    /**
     * 权限名称
     */
    private String permissionName;

    /**
     * 授权角色名称
     */
    private String roleName;

    /**
     * 操作集合
     */
    private List<String> actionList;


    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    public String getPermissionName() {
        return permissionName;
    }

    public void setPermissionName(String permissionName) {
        this.permissionName = permissionName;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<String> getActionList() {
        return actionList;
    }

    public void setActionList(List<String> actionList) {
        this.actionList = actionList;
    }
}
