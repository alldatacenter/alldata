package com.alibaba.sreworks.job.master.jobschedule.dag;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobTaskDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobTaskRepository;
import com.alibaba.sreworks.job.master.jobschedule.AbstractJobSchedule;
import com.alibaba.sreworks.job.master.jobschedule.JobScheduleService;
import com.alibaba.tesla.dag.model.domain.TcDag;
import com.alibaba.tesla.dag.model.domain.TcDagInst;
import com.alibaba.tesla.dag.model.repository.TcDagNodeRepository;
import com.alibaba.tesla.dag.model.repository.TcDagRepository;
import com.alibaba.tesla.dag.services.DagInstService;
import com.alibaba.tesla.dag.services.DagService;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class DagJobSchedule extends AbstractJobSchedule<DagJobScheduleConf> {

    public String type = "dag";

    @Autowired
    TcDagRepository dagRepository;

    @Autowired
    TcDagNodeRepository dagNodeRepository;

    @Autowired
    DagService dagService;

    @Autowired
    DagInstService dagInstService;

    @Autowired
    JobScheduleService jobScheduleService;

    @Autowired
    DagContentService dagContentService;

    @Autowired
    SreworksJobTaskRepository taskRepository;

    private String name(Long id) {
        return "job" + id;
    }

    @Override
    public Class<DagJobScheduleConf> getConfClass() {
        return DagJobScheduleConf.class;
    }

    @Override
    public JSONObject getConf(Long id) {
        TcDag dag = dagRepository.findFirstByAppIdAndName("tesla", name(id));
        DagJobScheduleConf conf = dagContentService.dagJobScheduleConf(dag.contentJson());
        JSONObject confJson = JSONObject.parseObject(JSONObject.toJSONString(conf));
        confJson.put("taskIdList", conf.getNodes().stream().map(node -> {
            Long taskId = node.getTaskId();
            return new SreworksJobTaskDTO(taskRepository.findFirstById(taskId));
        }).collect(Collectors.toList()));
        return confJson;
    }

    @Override
    public void create(Long id, DagJobScheduleConf conf) {
        TcDag dag = TcDag.builder()
            .gmtCreate(System.currentTimeMillis() / 1000)
            .gmtModified(System.currentTimeMillis() / 1000)
            .appId("tesla")
            .name(name(id))
            .content(dagContentService.dagContent(conf))
            .lastUpdateBy("")
            .build();
        dagRepository.saveAndFlush(dag);
    }

    @Override
    public void delete(Long id) {
        dagRepository.deleteByAppIdAndName("tesla", name(id));
    }

    @Override
    public void modify(Long id, DagJobScheduleConf conf) {
        TcDag dag = dagRepository.findFirstByAppIdAndName("tesla", name(id));
        dag.setContent(dagContentService.dagContent(conf));
        dagRepository.saveAndFlush(dag);
    }

    @Override
    public Long start(Long id, JSONObject varConf) throws Exception {
        return dagInstService.start(name(id), varConf, true);
    }

    @Override
    public void stop(Long dagInstId) {
        dagInstService.stop(dagInstId);
    }

    @Override
    public TcDagInst get(Long dagInstId) {
        return dagInstService.get(dagInstId);
    }

}
