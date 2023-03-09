package org.apache.griffin.core.metric.model;

import com.alibaba.fastjson.JSONObject;
import lombok.Data;

/**
 * @author AllDataDC
 * @date 2022/5/19
 */
@Data
public class MysqlMetrics {
    private Integer id;
    private String applicationId;
    private String tmst;
    private String jobName;
    private JSONObject metadata;
    private JSONObject value;


    public MysqlMetrics(){}

    public MysqlMetrics(Integer id, String applicationId, String tmst, String jobName, JSONObject metadata, JSONObject value) {
        this.id = id;
        this.applicationId = applicationId;
        this.tmst = tmst;
        this.jobName = jobName;
        this.metadata = metadata;
        this.value = value;
    }

    public MysqlMetrics(String tmst, String applicationId, String jobName, JSONObject metadata, JSONObject value) {
        this.tmst = tmst;
        this.applicationId = applicationId;
        this.jobName = jobName;
        this.metadata = metadata;
        this.value = value;
    }
}