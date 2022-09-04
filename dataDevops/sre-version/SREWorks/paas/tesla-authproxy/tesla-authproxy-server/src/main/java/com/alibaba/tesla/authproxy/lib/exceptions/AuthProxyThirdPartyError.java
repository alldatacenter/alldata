package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * 专有云 - 第三方业务组件异常
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AuthProxyThirdPartyError extends AuthProxyException {

    private String componentName;

    public AuthProxyThirdPartyError(String componentName, String message) {
        super(message);
        this.componentName = componentName;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String componentName) {
        this.componentName = componentName;
    }

}
