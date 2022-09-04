package com.alibaba.sreworks.job.worker.taskscene.risk;

import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobs;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobsRepository;
import com.alibaba.sreworks.job.utils.Requests;
import com.alibaba.sreworks.job.worker.taskscene.AbstractTaskScene;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.http.HttpResponse;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class RiskTaskScene extends AbstractTaskScene<RiskTaskSceneConf> {

    @Autowired
    ElasticTaskInstanceWithBlobsRepository taskInstanceRepository;

    public String type = "risk";

    @Override
    public Class<RiskTaskSceneConf> getConfClass() {
        return RiskTaskSceneConf.class;
    }

    @Override
    public void toSuccess(String taskInstanceId) throws Exception {
        ElasticTaskInstanceWithBlobs taskInstance = taskInstanceRepository.findFirstById(taskInstanceId);
        RiskTaskSceneConf taskSceneConf = getConf(taskInstance);

        String stdout = getStdout(taskInstance.getStdout());
        String url = "http://prod-health-health.sreworks.svc.cluster.local"
            + "/risk_instance/pushRisks?defId=" + taskSceneConf.getModelId();
        HttpResponse<String> response = Requests.post(url, null, null, parseOutputToArrayString(stdout));
        Requests.checkResponseStatus(response);
    }
}
