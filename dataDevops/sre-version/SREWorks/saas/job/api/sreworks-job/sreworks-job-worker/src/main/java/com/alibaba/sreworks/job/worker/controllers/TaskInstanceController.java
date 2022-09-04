package com.alibaba.sreworks.job.worker.controllers;

import com.alibaba.sreworks.job.worker.services.TaskInstanceService;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/taskInstance")
public class TaskInstanceController extends BaseController {

    @Autowired
    TaskInstanceService taskInstanceService;

    @RequestMapping(value = "start", method = RequestMethod.POST)
    public TeslaBaseResult start(String id) {

        taskInstanceService.start(id);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "stop", method = RequestMethod.DELETE)
    public TeslaBaseResult stop(String id) {

        taskInstanceService.stop(id);
        return buildSucceedResult("OK");

    }

}
