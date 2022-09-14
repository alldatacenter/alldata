package com.alibaba.sreworks.job.master.event;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJob;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobRepository;
import com.alibaba.sreworks.job.master.services.JobService;
import com.alibaba.sreworks.job.utils.StringUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class JobEventService {

    @Autowired
    JobService jobService;

    @Autowired
    SreworksJobRepository sreworksJobRepository;

    Map<Long, List<AbstractJobEventConsumer>> jobConsumersMap = new Hashtable<>();

    Map<Long, Long> jobGmtModifiedMap = new Hashtable<>();

    private List<AbstractJobEventConsumer> jobConsumers(SreworksJobDTO jobDTO) {
        List<AbstractJobEventConsumer> ret = new ArrayList<>();
        for (JobEventConf conf : jobDTO.getEventConf()) {
            switch (conf.getType()) {
                case KAFKA:
                    ret.add(new KafkaJobEventConsumer(conf));
                default:
                    break;
            }
        }
        return ret;
    }

    private void destroyConsumer(Long jobId) {
        jobConsumersMap.get(jobId).forEach(AbstractJobEventConsumer::close);
        jobConsumersMap.remove(jobId);
        jobGmtModifiedMap.remove(jobId);
    }

    private void cleanConsumerMap(List<SreworksJob> jobs) {
        List<Long> jobIds = jobs.stream().map(SreworksJob::getId).collect(Collectors.toList());
        new ArrayList<>(jobConsumersMap.keySet()).forEach(jobId -> {
            if (!jobIds.contains(jobId)) {
                destroyConsumer(jobId);
            }
        });
    }

    private void updateConsumerMap(List<SreworksJob> jobs) {
        jobs.forEach(job -> {
            Long jobId = job.getId();
            Long gmtModified = job.getGmtModified();

            List<Object> eventConf = StringUtil.isEmpty(job.getEventConf()) ? new ArrayList<>() : JSONObject.parseArray(job.getEventConf());
            if (!CollectionUtils.isEmpty(eventConf)) {
                if (!jobGmtModifiedMap.containsKey(jobId)) {
                    jobConsumersMap.put(jobId, jobConsumers(new SreworksJobDTO(job)));
                    jobGmtModifiedMap.put(jobId, gmtModified);
                }
                if (gmtModified > jobGmtModifiedMap.get(jobId)) {
                    jobConsumersMap.get(jobId).forEach(AbstractJobEventConsumer::close);
                    jobConsumersMap.put(jobId, jobConsumers(new SreworksJobDTO(job)));
                    jobGmtModifiedMap.put(jobId, gmtModified);
                }
            } else {
                if (jobGmtModifiedMap.containsKey(jobId)) {
                    destroyConsumer(jobId);
                }
            }
        });
    }

    @Scheduled(fixedRate = 1000)
    void init() {
        List<SreworksJob> jobs = sreworksJobRepository.findAll();
        cleanConsumerMap(jobs);
        updateConsumerMap(jobs);
    }

    @Scheduled(fixedRate = 100)
    void consume() {
        jobConsumersMap.forEach((jobId, consumers) -> {
            consumers.forEach(consumer -> {
                List<JSONObject> records = consumer.poll();
                for (JSONObject record : records) {
                    String traceId = record.getString("traceId");
//                    String spanId = record.getString("spanId");
                    try {
                        jobService.start(jobId, record, null, Collections.singletonList(traceId), "event");
                    } catch (Exception e) {
                        log.error("", e);
                    }
                }
            });
        });
    }

}
