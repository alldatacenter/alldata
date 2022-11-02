package com.alibaba.tesla.authproxy.lib.exceptions;

public class PrivateSmsSignatureForbidden extends AuthProxyException {

    public PrivateSmsSignatureForbidden() {
        super("Invalid signature");
    }

}
