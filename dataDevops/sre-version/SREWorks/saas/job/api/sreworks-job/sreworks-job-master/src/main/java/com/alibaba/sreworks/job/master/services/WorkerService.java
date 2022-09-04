package com.alibaba.sreworks.job.master.services;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.master.domain.DO.SreworksJobWorker;
import com.alibaba.sreworks.job.master.domain.DTO.SreworksJobWorkerDTO;
import com.alibaba.sreworks.job.master.domain.repository.SreworksJobWorkerRepository;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstance;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceRepository;
import com.alibaba.sreworks.job.utils.JsonUtil;
import com.alibaba.sreworks.job.utils.Requests;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class WorkerService {

    @Autowired
    SreworksJobWorkerRepository workerRepository;

    @Autowired
    ElasticTaskInstanceRepository taskInstanceRepository;

    @Scheduled(fixedRate = 1000)
    public void fixedSchedule() {
        workerRepository.deleteAllByGmtModifiedBefore(System.currentTimeMillis() - 20000);
    }

    public List<Object> listExecType() {

        List<SreworksJobWorker> workers = workerRepository.findAll();
        return workers.stream()
            .map(SreworksJobWorker::getExecTypeList)
            .map(JSONObject::parseArray)
            .flatMap(List::stream)
            .distinct()
            .collect(Collectors.toList());

    }

    public SreworksJobWorkerDTO getByExecType(String execType) {

        List<SreworksJobWorkerDTO> workerDTOList = workerRepository.findAllByEnable(1).stream()
            .map(SreworksJobWorkerDTO::new)
            .filter(x -> x.getExecTypeList().contains(execType))
            .collect(Collectors.toList());
        Collections.shuffle(workerDTOList);
        return workerDTOList.get(0);

    }

    public void startInstance(String address, String taskInstanceId)
        throws Exception {

        String url = address + "/taskInstance/start";
        HttpResponse<String> response = Requests.post(
            url,
            null,
            JsonUtil.map("id", taskInstanceId),
            null
        );
        Requests.checkResponseStatus(response);

    }

    public void stopInstance(String taskInstanceId) throws Exception {
        ElasticTaskInstance taskInstance = taskInstanceRepository.findFirstById(taskInstanceId);
        String url = taskInstance.getAddress() + "/taskInstance/stop";
        HttpResponse<String> response = Requests.delete(url, null, JsonUtil.map("id", taskInstanceId));
        Requests.checkResponseStatus(response);
    }

}
