package com.alibaba.tesla.authproxy.util.audit;

public enum AuditOutcomeEnum {

    SUCCESS,
    FAILURE,
    PENDING,
    NULL;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
