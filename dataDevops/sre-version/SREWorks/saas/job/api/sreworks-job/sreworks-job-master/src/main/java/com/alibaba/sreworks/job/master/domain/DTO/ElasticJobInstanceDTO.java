package com.alibaba.sreworks.job.master.domain.DTO;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.ElasticJobInstance;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Data
@Slf4j
public class ElasticJobInstanceDTO {

    private String id;

    private Long gmtCreate;

    private Long gmtExecute;

    private Long gmtStop;

    private Long gmtEnd;

    private SreworksJobDTO job;

    private JSONObject varConf;

    private Long scheduleInstanceId;

    private Object scheduleInstance;

    private JobInstanceStatus status;

    private List<String> tags;

    private List<String> traceIds;

    public ElasticJobInstanceDTO(ElasticJobInstance jobInstance) {
        this.id = jobInstance.getId();
        this.gmtCreate = jobInstance.getGmtCreate();
        this.gmtExecute = jobInstance.getGmtExecute();
        this.gmtStop = jobInstance.getGmtStop();
        this.gmtEnd = jobInstance.getGmtEnd();
        this.job = JSONObject.parseObject(jobInstance.getJob(), SreworksJobDTO.class);
        this.varConf = JSONObject.parseObject(jobInstance.getVarConf());
        this.scheduleInstanceId = jobInstance.getScheduleInstanceId();
        this.status = JobInstanceStatus.valueOf(jobInstance.getStatus());
        this.tags = jobInstance.getTags();
        this.traceIds = jobInstance.getTraceIds();
    }

}
