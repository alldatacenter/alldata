package com.alibaba.sreworks.job.master.params;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Data
@Slf4j
public class JobEditScheduleParam {

    private String scheduleType;

    private JSONObject scheduleConf;

    private JSONObject varConf;

    public void patchJob(SreworksJob job) {
        job.setGmtModified(System.currentTimeMillis());
        job.setScheduleType(getScheduleType());
        job.setVarConf(JSONObject.toJSONString(getVarConf()));
    }

}
