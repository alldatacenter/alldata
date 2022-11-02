package com.alibaba.tesla.authproxy.lib.shiro;

import org.apache.shiro.authc.AuthenticationToken;

/**
 * 权代 Token 标识
 *
 * @author yaoxing.gyx@alibaba-inc.com
 */
public class AuthProxyToken implements AuthenticationToken {

    private AuthProxyPrincipal principal;

    public AuthProxyToken(AuthProxyPrincipal principal) {
        this.principal = principal;
    }

    @Override
    public Object getPrincipal() {
        return principal;
    }

    @Override
    public Object getCredentials() {
        return principal;
    }
}
