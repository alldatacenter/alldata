package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;
import java.util.List;

/**
 * 专有云权限拉取角色列表参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivatePermissionRoleAccountDeleteParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{private.validation.required.roleName}")
    private String roleName;

    private List<String> aliyunId;

    public void cleanSelf() {
        this.roleName = roleName.trim();
    }

    public String getRoleName() {
        return roleName;
    }

    public void setRoleName(String roleName) {
        this.roleName = roleName;
    }

    public List<String> getAliyunId() {
        return aliyunId;
    }

    public void setAliyunId(List<String> aliyunId) {
        this.aliyunId = aliyunId;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
