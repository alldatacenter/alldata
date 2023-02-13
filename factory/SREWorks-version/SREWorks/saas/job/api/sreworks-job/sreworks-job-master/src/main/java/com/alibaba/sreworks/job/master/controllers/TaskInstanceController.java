package com.alibaba.sreworks.job.master.controllers;

import com.alibaba.sreworks.job.master.services.TaskInstanceService;
import com.alibaba.sreworks.job.master.services.WorkerService;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstance;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceDTO;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceRepository;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobsRepository;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.tesla.common.base.TeslaBaseResult;
import com.alibaba.tesla.web.controller.BaseController;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.stream.Collectors;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/taskInstance")
public class TaskInstanceController extends BaseController {

    @Autowired
    TaskInstanceService taskInstanceService;

    @Autowired
    WorkerService workerService;

    @Autowired
    ElasticTaskInstanceRepository elasticTaskInstanceRepository;

    @Autowired
    ElasticTaskInstanceWithBlobsRepository elasticTaskInstanceWithBlobsRepository;

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(String taskIdListString, String statusListString, Integer page, Integer pageSize) {

        Page<ElasticTaskInstance> taskInstancePage = taskInstanceService.list(
            taskIdListString, statusListString, page, pageSize);
        return buildSucceedResult(JsonUtil.map(
            "total", taskInstancePage.getTotalElements(),
            "items", taskInstancePage.getContent().stream()
                .map(ElasticTaskInstanceDTO::new).collect(Collectors.toList())
        ));

    }

    @RequestMapping(value = "list2", method = RequestMethod.GET)
    public TeslaBaseResult list2(String taskIdListString, String statusListString, Integer page, Integer pageSize,
        String id) {

        SearchHits<ElasticTaskInstance> searchHits = taskInstanceService.list2(
            taskIdListString, statusListString, page, pageSize, id);
        return buildSucceedResult(JsonUtil.map(
            "total", searchHits.getTotalHits(),
            "items", searchHits.stream().map(SearchHit::getContent)
                .map(ElasticTaskInstanceDTO::new).collect(Collectors.toList())
        ));

    }

    @RequestMapping(value = "listByJobInstanceId", method = RequestMethod.GET)
    public TeslaBaseResult listByJobInstanceId(String jobInstanceId) {

        return buildSucceedResult(elasticTaskInstanceWithBlobsRepository.findAllByJobInstanceId(jobInstanceId));

    }

    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(String id) {

        return buildSucceedResult(
            taskInstanceService.get(id)
        );

    }

    @RequestMapping(value = "getWithBlobs", method = RequestMethod.GET)
    public TeslaBaseResult getWithBlobs(String id) {

        return buildSucceedResult(
            taskInstanceService.getWithBlobs(id)
        );

    }

    @RequestMapping(value = "stop", method = RequestMethod.DELETE)
    public TeslaBaseResult stop(String id) throws Exception {

        workerService.stopInstance(id);
        return buildSucceedResult("OK");

    }

}
