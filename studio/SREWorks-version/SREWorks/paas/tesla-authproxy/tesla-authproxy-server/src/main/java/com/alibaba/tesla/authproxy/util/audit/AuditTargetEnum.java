package com.alibaba.tesla.authproxy.util.audit;

public enum AuditTargetEnum {

    COMPUTE,
    STORAGE,
    NETWORK,
    DATA,
    NULL;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
