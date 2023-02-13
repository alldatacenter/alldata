package com.alibaba.sreworks.job.master.jobtrigger.quartz;

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
@RequestMapping("/quartz")
public class QuartzController extends BaseController {

    @Autowired
    QuartzService quartzService;

    @RequestMapping(value = "checkCron", method = RequestMethod.GET)
    public TeslaBaseResult checkCron(String cron) throws Exception {

        quartzService.checkCron(cron);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "getNextTriggerTime", method = RequestMethod.GET)
    public TeslaBaseResult getNextTriggerTime(String cron, Integer size) throws Exception {

        if (size == null) {
            size = 5;
        }
        return buildSucceedResult(quartzService.getNextTriggerTime(cron, size));

    }

}
