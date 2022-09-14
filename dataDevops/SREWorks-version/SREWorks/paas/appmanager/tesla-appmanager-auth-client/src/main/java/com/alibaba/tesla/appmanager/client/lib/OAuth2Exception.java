package com.alibaba.tesla.appmanager.client.lib;

/**
 * 全局异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class OAuth2Exception extends RuntimeException {

    protected String errorMessage;

    public OAuth2Exception() {
        this.errorMessage = null;
    }

    public OAuth2Exception(String errorMessage) {
        super(errorMessage);
        this.errorMessage = errorMessage;
    }

    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public String toString() {
        return errorMessage;
    }
}
