package com.alibaba.sreworks.job.worker.taskscene.incident;

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
public class IncidentTaskScene extends AbstractTaskScene<IncidentTaskSceneConf> {

    public String type = "incident";

    private String healthEndpoint = "http://prod-health-health.sreworks.svc.cluster.local";

    @Autowired
    ElasticTaskInstanceWithBlobsRepository taskInstanceRepository;

    @Override
    public Class<IncidentTaskSceneConf> getConfClass() {
        return IncidentTaskSceneConf.class;
    }

    @Override
    public void toSuccess(String taskInstanceId) throws Exception {
        ElasticTaskInstanceWithBlobs taskInstance = taskInstanceRepository.findFirstById(taskInstanceId);
        IncidentTaskSceneConf taskSceneConf = getConf(taskInstance);

        String stdout = getStdout(taskInstance.getStdout());
        List<JSONObject> results = parseOutputToList(stdout);
        for (JSONObject result : results) {
            // 自愈异常实例
            JSONObject currentIncident = result.getJSONObject("currentIncident");
            // 更新自愈实例的自愈开始时间
            String idUrl = String.format("%s/incident_instance/updateIncidentHealingById?id=%s", healthEndpoint, currentIncident.getLong("id"));
            JSONObject updateBody = new JSONObject();
            updateBody.put("selfHealingStartTs", currentIncident.getLong("gmtSelfHealingStart"));
            updateBody.put("selfHealingEndTs", currentIncident.getLong("gmtSelfHealingEnd"));
            updateBody.put("selfHealingStatus", currentIncident.getString("selfHealingStatus"));
            HttpResponse<String> response1 = Requests.post(idUrl, null, null, JSONObject.toJSONString(updateBody));
            Requests.checkResponseStatus(response1);

            // 更新trace链上的实例自愈状态
            String traceUrl = String.format("%s/incident_instance/updateIncidentSelfHealingByTrace?traceId=%s", healthEndpoint, currentIncident.getString("traceId"));
            updateBody.remove("selfHealingStartTs");
            HttpResponse<String> response2 = Requests.post(traceUrl, null, null, JSONObject.toJSONString(updateBody));
            Requests.checkResponseStatus(response2);

            // 产出异常实例
            JSONObject nextIncident = result.getJSONObject("nextIncident");
            if (!CollectionUtils.isEmpty(nextIncident) && taskSceneConf.getModelId() != null) {
                Long nowTs = System.currentTimeMillis();
                JSONObject incidentInstance = new JSONObject();
                incidentInstance.put("defId", taskSceneConf.getModelId());
                incidentInstance.put("appInstanceId", nextIncident.getString("appInstanceId"));
                incidentInstance.put("appComponentInstanceId", nextIncident.getString("appComponentInstanceId"));
                incidentInstance.put("occurTs", nowTs);
                incidentInstance.put("source", "异常诊断");
                incidentInstance.put("cause", nextIncident.getString("cause"));
                incidentInstance.put("traceId", currentIncident.getString("traceId"));
                incidentInstance.put("spanId", currentIncident.getLong("spanId") + 1);
                incidentInstance.put("selfHealingStartTs", nowTs);
                incidentInstance.put("selfHealingStatus", "WAITING");
                incidentInstance.put("options", nextIncident.getString("options"));
                String url = String.format("%s/incident_instance/pushIncident?defId=%s", healthEndpoint, taskSceneConf.getModelId());
                HttpResponse<String> response = Requests.post(url, null, null, JSONObject.toJSONString(incidentInstance));
                Requests.checkResponseStatus(response);
            }
        }
    }
}
