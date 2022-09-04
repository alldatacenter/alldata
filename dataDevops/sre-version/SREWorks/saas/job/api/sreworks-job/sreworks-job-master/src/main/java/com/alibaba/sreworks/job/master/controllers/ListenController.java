package com.alibaba.sreworks.job.master.controllers;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJobWorker;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobWorkerDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobWorkerRepository;
import com.alibaba.sreworks.job.master.params.ListenWorkerParam;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/listen")
public class ListenController extends BaseController {

    @Autowired
    SreworksJobWorkerRepository workerRepository;

    @RequestMapping(value = "worker", method = RequestMethod.POST)
    public TeslaBaseResult register(@RequestBody ListenWorkerParam param) {

        SreworksJobWorker worker = workerRepository.findFirstByAddress(param.getAddress());
        if (worker == null) {
            worker = SreworksJobWorker.builder()
                .gmtCreate(System.currentTimeMillis())
                .groupName("DEFAULT")
                .address(param.getAddress())
                .enable(1)
                .poolSize(10)
                .build();
        }
        worker.setGmtModified(System.currentTimeMillis());
        worker.setExecTypeList(JSONObject.toJSONString(param.getExecTypeList()));
        workerRepository.saveAndFlush(worker);
        return buildSucceedResult(new SreworksJobWorkerDTO(worker));

    }

}
