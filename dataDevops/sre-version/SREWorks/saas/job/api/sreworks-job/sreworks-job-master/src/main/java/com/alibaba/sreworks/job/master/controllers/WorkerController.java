package com.alibaba.sreworks.job.master.controllers;

import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobWorkerDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobWorkerRepository;
import com.alibaba.sreworks.job.master.services.WorkerService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/worker")
public class WorkerController extends BaseController {

    @Autowired
    WorkerService workerService;

    @Autowired
    SreworksJobWorkerRepository workerRepository;

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list() {

        return buildSucceedResult(
            workerRepository.findAll().stream().map(SreworksJobWorkerDTO::new).collect(Collectors.toList())
        );

    }

    @RequestMapping(value = "listExecType", method = RequestMethod.GET)
    public TeslaBaseResult listExecType() {

        return buildSucceedResult(workerService.listExecType());

    }

}
