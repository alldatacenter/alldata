package com.aliyun.oss.model;

import java.io.Serializable;

public class TransferAcceleration extends GenericResult implements Serializable {
    private static final long serialVersionUID = 2596858103633662949L;
    private boolean enabled;

    public TransferAcceleration(boolean enabled) {
        super();
        setEnabled(enabled);
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}