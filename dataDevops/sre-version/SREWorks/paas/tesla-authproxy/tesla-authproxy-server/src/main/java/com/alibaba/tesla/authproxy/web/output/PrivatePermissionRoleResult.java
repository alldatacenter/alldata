package com.alibaba.tesla.authproxy.web.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 专有云权限拉取角色列表返回
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivatePermissionRoleResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private List<PrivatePermissionRoleItem> results;

    public PrivatePermissionRoleResult() {
        this.results = new ArrayList<>();
    }

    public List<PrivatePermissionRoleItem> getResults() {
        return results;
    }

    public void addResult(PrivatePermissionRoleItem item) {
        this.results.add(item);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonInclude(Include.NON_NULL)
    public static class PrivatePermissionRoleItem implements Serializable {

        public static final long serialVersionUID = 1L;

        private String bid;

        private String roleId;

        private String roleName;

        private String roleType;

        private Date gmtModified;

        private String owner;

        private String ownerName;

        private String ownerType;

        private String description;

        private Date gmtExpired;

        public String getBid() {
            return bid;
        }

        public void setBid(String bid) {
            this.bid = bid;
        }

        public String getRoleId() {
            return roleId;
        }

        public void setRoleId(String roleId) {
            this.roleId = roleId;
        }

        public String getRoleName() {
            return roleName;
        }

        public void setRoleName(String roleName) {
            this.roleName = roleName;
        }

        public String getRoleType() {
            return roleType;
        }

        public void setRoleType(String roleType) {
            this.roleType = roleType;
        }

        public Date getGmtModified() {
            return gmtModified;
        }

        public void setGmtModified(Date gmtModified) {
            this.gmtModified = gmtModified;
        }

        public String getOwner() {
            return owner;
        }

        public void setOwner(String owner) {
            this.owner = owner;
        }

        public String getOwnerName() {
            return ownerName;
        }

        public void setOwnerName(String ownerName) {
            this.ownerName = ownerName;
        }

        public String getOwnerType() {
            return ownerType;
        }

        public void setOwnerType(String ownerType) {
            this.ownerType = ownerType;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public Date getGmtExpired() {
            return gmtExpired;
        }

        public void setGmtExpired(Date gmtExpired) {
            this.gmtExpired = gmtExpired;
        }

    }

}
