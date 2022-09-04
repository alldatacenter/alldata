package com.alibaba.sreworks.job.master.domain.DTO;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJobWorker;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @author jinghua.yjh
 */
@Slf4j
@Data
public class SreworksJobWorkerDTO {

    private Long id;

    private Long gmtCreate;

    private Long gmtModified;

    private String groupName;

    private String address;

    private List<String> execTypeList;

    private boolean enable;

    private Integer poolSize;

    public SreworksJobWorkerDTO(SreworksJobWorker worker) {
        this.id = worker.getId();
        this.gmtCreate = worker.getGmtCreate();
        this.gmtModified = worker.getGmtModified();
        this.groupName = worker.getGroupName();
        this.address = worker.getAddress();
        this.execTypeList = JSONObject.parseArray(worker.getExecTypeList()).toJavaList(String.class);
        this.enable = worker.getEnable() == 1;
        this.poolSize = worker.getPoolSize();
    }

}
