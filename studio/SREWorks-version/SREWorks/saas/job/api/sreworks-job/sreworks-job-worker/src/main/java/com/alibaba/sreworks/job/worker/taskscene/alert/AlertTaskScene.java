package com.alibaba.sreworks.job.worker.taskscene.alert;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobs;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobsRepository;
import com.alibaba.sreworks.job.utils.Requests;
import com.alibaba.sreworks.job.worker.taskscene.AbstractTaskScene;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.net.http.HttpResponse;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class AlertTaskScene extends AbstractTaskScene<AlertTaskSceneConf> {

    public String type = "alert";

    private String healthEndpoint = "http://prod-health-health.sreworks.svc.cluster.local";

    @Autowired
    ElasticTaskInstanceWithBlobsRepository taskInstanceRepository;

    @Override
    public Class<AlertTaskSceneConf> getConfClass() {
        return AlertTaskSceneConf.class;
    }

    @Override
    public void toSuccess(String taskInstanceId) throws Exception {
        ElasticTaskInstanceWithBlobs taskInstance = taskInstanceRepository.findFirstById(taskInstanceId);
        AlertTaskSceneConf taskSceneConf = getConf(taskInstance);

        String stdout = getStdout(taskInstance.getStdout());
        List<JSONObject> results = parseOutputToList(stdout);

        for (JSONObject result : results) {
            JSONObject nextIncident = result.getJSONObject("nextIncident");
            if (!CollectionUtils.isEmpty(nextIncident) && taskSceneConf.getModelId() != null) {
                JSONObject incidentInstance = new JSONObject();
                incidentInstance.put("defId", taskSceneConf.getModelId());
                incidentInstance.put("appInstanceId", nextIncident.getString("appInstanceId"));
                incidentInstance.put("appComponentInstanceId", nextIncident.getString("appComponentInstanceId"));
                incidentInstance.put("occurTs", System.currentTimeMillis());
                incidentInstance.put("source", "告警分析");
                incidentInstance.put("cause", nextIncident.getString("cause"));
                incidentInstance.put("options", nextIncident.getString("options"));
                String url = String.format("%s/incident_instance/pushIncident?defId=%s", healthEndpoint, taskSceneConf.getModelId());
                HttpResponse<String> response = Requests.post(url, null, null, JSONObject.toJSONString(incidentInstance));
                Requests.checkResponseStatus(response);
            }
        }
    }
}
