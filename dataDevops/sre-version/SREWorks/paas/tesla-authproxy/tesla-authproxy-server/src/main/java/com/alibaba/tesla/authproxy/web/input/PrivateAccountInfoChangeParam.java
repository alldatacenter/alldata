package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import java.io.Serializable;

/**
 * 云账号信息修改 API 参数
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountInfoChangeParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{private.validation.required.aliyunId}")
    private String aliyunId;

    @NotEmpty(message = "{private.validation.required.phone}")
    private String phone;

    public void cleanSelf() {
        this.aliyunId = aliyunId.trim();
    }

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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
