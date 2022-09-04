package com.alibaba.tesla.authproxy.web.input;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.validator.constraints.NotEmpty;

import javax.validation.constraints.NotNull;
import java.io.Serializable;

/**
 * 专有云 - 账户锁定与解锁
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountLockParam implements Serializable {

    public static final long serialVersionUID = 1L;

    @NotEmpty(message = "{private.validation.required.aliyunId}")
    private String aliyunId;

    @NotNull(message = "{private.validation.required.isLock}")
    private Boolean isLock;

    public void cleanSelf() {
        this.aliyunId = aliyunId.trim();
    }

    public String getAliyunId() {
        return aliyunId;
    }

    public void setAliyunId(String aliyunId) {
        this.aliyunId = aliyunId;
    }

    public Boolean getIsLock() {
        return isLock;
    }

    public void setIsLock(Boolean isLock) {
        this.isLock = isLock;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

}
