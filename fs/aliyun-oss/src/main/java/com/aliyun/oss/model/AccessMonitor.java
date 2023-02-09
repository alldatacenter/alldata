package com.aliyun.oss.model;

import java.io.Serializable;

public class AccessMonitor extends GenericResult implements Serializable {
    public static enum AccessMonitorStatus {
        Enabled, // The Rule is enabled.
        Disabled // The rule is disabled.
    };

    private static final long serialVersionUID = 4379230587752372898L;
    private String status;

    public AccessMonitor(String status) {
        this.status = status;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
