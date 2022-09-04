package com.alibaba.tesla.authproxy.util.audit;

public enum AuditActionEnum {

    CREATE,
    DELETE,
    UPDATE,
    NOTIFY,
    SELECT,
    ENABLE,
    DISABLE,
    NULL;

    @Override
    public String toString() {
        return super.toString().toLowerCase();
    }

}
