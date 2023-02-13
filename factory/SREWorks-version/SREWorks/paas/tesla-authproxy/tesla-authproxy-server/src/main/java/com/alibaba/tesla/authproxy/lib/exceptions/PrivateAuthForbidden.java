package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * 专有云验证权限不足异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class PrivateAuthForbidden extends AuthProxyException {

    private String resourcePath;

    public PrivateAuthForbidden() {
        resourcePath = "";
    }

    public PrivateAuthForbidden(String message) {
        super(message);
    }

    public PrivateAuthForbidden(String message, String resourcePath) {
        super(message);
        this.resourcePath = resourcePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public void setResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
    }

}
