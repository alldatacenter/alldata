package com.alibaba.sreworks.job.master.common;

public enum JobTriggerType {

    NORMAL("normal"),
    CRON("cron");

    private String type;

    JobTriggerType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }
}
