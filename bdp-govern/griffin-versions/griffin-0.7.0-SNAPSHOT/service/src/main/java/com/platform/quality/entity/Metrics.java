package com.platform.quality.entity;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * @author wlhbdp
 * @date 2022/5/19
 */
@Data
public class Metrics {
    private Integer id;
    private String applicationId;
    private String tmst;
    private String jobName;
    private JSONObject metadata;
    private JSONObject value;


    public Metrics(){}

    public Metrics(Integer id, String applicationId, String tmst, String jobName, JSONObject metadata, JSONObject value) {
        this.id = id;
        this.applicationId = applicationId;
        this.tmst = tmst;
        this.jobName = jobName;
        this.metadata = metadata;
        this.value = value;
    }

    public Metrics(String tmst, String applicationId, String jobName, JSONObject metadata, JSONObject value) {
        this.tmst = tmst;
        this.applicationId = applicationId;
        this.jobName = jobName;
        this.metadata = metadata;
        this.value = value;
    }
}