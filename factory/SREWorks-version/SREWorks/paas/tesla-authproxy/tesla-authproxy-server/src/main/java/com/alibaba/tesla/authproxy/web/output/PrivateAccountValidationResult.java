package com.alibaba.tesla.authproxy.web.output;

import java.io.Serializable;

/**
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAccountValidationResult implements Serializable {

    public static final long serialVersionUID = 1L;

    private String validation;

    public String getValidation() {
        return validation;
    }

    public void setValidation(String validation) {
        this.validation = validation;
    }

}
