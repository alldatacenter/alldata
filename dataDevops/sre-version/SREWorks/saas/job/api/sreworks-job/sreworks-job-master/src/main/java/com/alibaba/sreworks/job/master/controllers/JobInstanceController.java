package com.alibaba.sreworks.job.master.controllers;

import com.alibaba.sreworks.job.master.domain.DO.ElasticJobInstance;
import com.alibaba.sreworks.job.master.domain.DTO.ElasticJobInstanceDTO;
import com.alibaba.sreworks.job.master.domain.DTO.JobInstanceStatus;
import com.alibaba.sreworks.job.master.domain.repository.ElasticJobInstanceRepository;
import com.alibaba.sreworks.job.master.services.JobInstanceService;
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

import static com.alibaba.sreworks.job.utils.PageUtil.pageable;

/**
 * @author jinghua.yjh
 */
@Slf4j
@RestController
@RequestMapping("/jobInstance")
public class JobInstanceController extends BaseController {

    @Autowired
    JobInstanceService jobInstanceService;

    @Autowired
    ElasticJobInstanceRepository jobInstanceRepository;

    @RequestMapping(value = "listStatus", method = RequestMethod.GET)
    public TeslaBaseResult listStatus() {
        return buildSucceedResult(JobInstanceStatus.values());
    }

    @RequestMapping(value = "list", method = RequestMethod.GET)
    public TeslaBaseResult list(Long jobId, String status, Integer page, Integer pageSize) {

        Page<ElasticJobInstance> jobInstancePage = jobInstanceService.list(jobId,
            status, pageable(page, pageSize));
        return buildSucceedResult(JsonUtil.map(
            "total", jobInstancePage.getTotalElements(),
            "items", jobInstancePage.getContent().stream()
                .map(ElasticJobInstanceDTO::new).collect(Collectors.toList())
        ));

    }

    @RequestMapping(value = "list2", method = RequestMethod.GET)
    public TeslaBaseResult list2(Long jobId, String status, Integer page, Integer pageSize, String id) {

        SearchHits<ElasticJobInstance> searchHits = jobInstanceService
            .list2(jobId, status, pageable(page, pageSize), id);
        return buildSucceedResult(JsonUtil.map(
            "total", searchHits.getTotalHits(),
            "items", searchHits.stream().map(SearchHit::getContent)
                .map(ElasticJobInstanceDTO::new).collect(Collectors.toList())
        ));

    }

    @RequestMapping(value = "listByTraceId", method = RequestMethod.GET)
    public TeslaBaseResult listByTraceId(String traceId, Integer page, Integer pageSize) {

        SearchHits<ElasticJobInstance> searchHits = jobInstanceService
            .listByTraceId(traceId, pageable(page, pageSize));
        return buildSucceedResult(JsonUtil.map(
            "total", searchHits.getTotalHits(),
            "items", searchHits.stream().map(SearchHit::getContent)
                .map(ElasticJobInstanceDTO::new).collect(Collectors.toList())
        ));

    }

    @RequestMapping(value = "get", method = RequestMethod.GET)
    public TeslaBaseResult get(String id) throws Exception {

        return buildSucceedResult(jobInstanceService.get(id));

    }

    @RequestMapping(value = "stop", method = RequestMethod.DELETE)
    public TeslaBaseResult stop(String id) throws Exception {

        jobInstanceService.stop(id);
        return buildSucceedResult("OK");

    }

    @RequestMapping(value = "distinctTag", method = RequestMethod.GET)
    public TeslaBaseResult distinctTag(Long jobId, Long stime, Long etime) {
        stime = stime == null ? Long.MIN_VALUE : stime;
        etime = etime == null ? Long.MAX_VALUE : etime;
        return buildSucceedResult(jobInstanceService.distinctOrderByMaxGmtCreate(
            jobId, stime, etime, "tag", "tags"));
    }

    @RequestMapping(value = "byTag", method = RequestMethod.GET)
    public TeslaBaseResult byTag(Long jobId, String tag, Long stime, Long etime, Integer page, Integer pageSize) {
        stime = stime == null ? Long.MIN_VALUE : stime;
        etime = etime == null ? Long.MAX_VALUE : etime;
        Page<ElasticJobInstance> jobInstancePage = jobInstanceRepository
            .findAllByJobIdAndTagsAndGmtCreateBetweenOrderByGmtCreateDesc(
                jobId, tag, stime, etime, pageable(page, pageSize)
            );
        return buildSucceedResult(JsonUtil.map(
            "total", jobInstancePage.getTotalElements(),
            "items", jobInstancePage.getContent().stream().map(ElasticJobInstanceDTO::new).collect(Collectors.toList())
        ));
    }

    @RequestMapping(value = "distinctTraceId", method = RequestMethod.GET)
    public TeslaBaseResult distinctTraceId(Long jobId, Long stime, Long etime) {
        stime = stime == null ? Long.MIN_VALUE : stime;
        etime = etime == null ? Long.MAX_VALUE : etime;
        return buildSucceedResult(jobInstanceService.distinctOrderByMaxGmtCreate(
            jobId, stime, etime, "traceId", "traceIds"));
    }

    @RequestMapping(value = "byTraceId", method = RequestMethod.GET)
    public TeslaBaseResult byTraceId(
        Long jobId, String traceId, Long stime, Long etime, Integer page, Integer pageSize) {

        stime = stime == null ? Long.MIN_VALUE : stime;
        etime = etime == null ? Long.MAX_VALUE : etime;
        Page<ElasticJobInstance> jobInstancePage = jobInstanceRepository
            .findAllByJobIdAndTraceIdsAndGmtCreateBetweenOrderByGmtCreateDesc(
                jobId, traceId, stime, etime, pageable(page, pageSize)
            );
        return buildSucceedResult(JsonUtil.map(
            "total", jobInstancePage.getTotalElements(),
            "items", jobInstancePage.getContent().stream().map(ElasticJobInstanceDTO::new).collect(Collectors.toList())
        ));
    }

    @RequestMapping(value = "groupByStatus", method = RequestMethod.GET)
    public TeslaBaseResult groupByStatus(String sceneType, Long stime, Long etime) {

        if (stime == null) {
            stime = System.currentTimeMillis() / 1000 - 86400 * 7;
        }
        if (etime == null) {
            etime = System.currentTimeMillis() / 1000;
        }
        return buildSucceedResult(jobInstanceService.groupByStatus(sceneType, stime, etime));

    }

    @RequestMapping(value = "groupByTraceId", method = RequestMethod.GET)
    public TeslaBaseResult groupByTraceId(Long stime, Long etime) {

        stime = stime == null ? Long.MIN_VALUE : stime;
        etime = etime == null ? Long.MAX_VALUE : etime;
        return buildSucceedResult(jobInstanceService.groupByTraceId(stime, etime));

    }

}
