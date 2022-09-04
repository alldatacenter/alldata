package com.alibaba.tesla.authproxy.web.output;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import org.apache.commons.lang3.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * 专有云权限角色可授权云账号列表返回
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivatePermissionRoleAccountResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private List<PrivatePermissionRoleAccountItem> results;

    public PrivatePermissionRoleAccountResult() {
        this.results = new ArrayList<>();
    }

    public List<PrivatePermissionRoleAccountItem> getResults() {
        return results;
    }

    public void addResult(PrivatePermissionRoleAccountItem item) {
        this.results.add(item);
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @JsonInclude(Include.NON_NULL)
    public static class PrivatePermissionRoleAccountItem implements Serializable {

        public static final long serialVersionUID = 1L;

        private String aliyunId;

        private String phone;

        private String status;

        public String getAliyunId() {
            return aliyunId;
        }

        public void setAliyunId(String aliyunId) {
            this.aliyunId = aliyunId;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

    }

}
