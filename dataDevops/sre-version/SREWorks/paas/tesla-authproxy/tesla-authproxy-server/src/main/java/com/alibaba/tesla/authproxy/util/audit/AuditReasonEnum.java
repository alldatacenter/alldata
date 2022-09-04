package com.alibaba.tesla.authproxy.util.audit;

public enum AuditReasonEnum {

    UNAUTHORIZED,
    AUTHORIZED,
    NULL;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
