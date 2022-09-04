package com.alibaba.tesla.authproxy.model.vo;

/**
 * <p>Description: 角色信息 <／p>
 * <p>Copyright: alibaba (c) 2017<／p>
 * <p>Company: alibaba <／p>
 *
 * @author tandong.td@alibaba-inc.com
 * @version 1.0
 * @date 2017/5/7 下午2:44
 */
public class TeslaRoleVO {

    /**
     * 角色编码
     */
    private String roleCode;

    /**
     * 角色名称
     */
    private String roleName;

    /**
     * 角色归属人
     */
    private String roleOwner;

    /**
     * 描述
     */
    private String memo;

    public String getRoleCode() {
        return roleCode;
    }

    public void setRoleCode(String roleCode) {
        this.roleCode = roleCode;
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getRoleOwner() {
        return roleOwner;
    }

    public void setRoleOwner(String roleOwner) {
        this.roleOwner = roleOwner;
    }
}
