package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 专有云权限拉取角色列表参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivatePermissionRoleAccountParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{private.validation.required.roleName}")
    private String roleName;

    private String filter = "all";

    public void cleanSelf() {
        this.roleName = roleName.trim();
        this.filter = filter.trim();
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
