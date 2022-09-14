package com.alibaba.tesla.authproxy.lib.exceptions;

/**
 * @author tandong
 * @Description:TODO
 * @date 2019/3/7 19:09
 */
public class PrivateOamRoleNotExitsException extends AuthProxyException {

    public PrivateOamRoleNotExitsException(String roleName) {
        super("oam role (" + roleName + ") not exits");
    }
}
