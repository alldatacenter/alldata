package com.alibaba.sreworks.job.worker.taskscene.collect;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobs;
import com.alibaba.sreworks.job.taskinstance.ElasticTaskInstanceWithBlobsRepository;
import com.alibaba.sreworks.job.utils.Requests;
import com.alibaba.sreworks.job.worker.taskscene.AbstractTaskScene;
import com.google.common.base.Preconditions;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.DigestUtils;

import java.net.http.HttpResponse;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@Service
@Slf4j
public class CollectTaskScene extends AbstractTaskScene<CollectTaskSceneConf> {

    @Autowired
    ElasticTaskInstanceWithBlobsRepository taskInstanceRepository;

    public String type = "collect";

    private String collectPushDataEndpoint = "http://prod-dataops-warehouse.sreworks-dataops";

    private String pmdbEndpoint = "http://prod-dataops-pmdb.sreworks-dataops.svc.cluster.local:80";

    @Override
    public Class<CollectTaskSceneConf> getConfClass() {
        return CollectTaskSceneConf.class;
    }

    @Override
    public void toSuccess(String taskInstanceId) throws Exception {
        ElasticTaskInstanceWithBlobs taskInstance = taskInstanceRepository.findFirstById(taskInstanceId);
        CollectTaskSceneConf taskSceneConf = getConf(taskInstance);

        String stdout = parseOutputToArrayString(getStdout(taskInstance.getStdout()));

        // 同步PMDB指标服务
        if (taskSceneConf.getRelatedMetricId() != null) {
            List<JSONObject> metricDatas = buildMetricDatas(taskSceneConf.getRelatedMetricId(), stdout);

            pushPmdb(taskSceneConf.getRelatedMetricId(), metricDatas, taskSceneConf.getIsPushQueue());
            stdout = JSONObject.toJSONString(metricDatas);
        }

        // 同步数仓url
        String dwUrl = buildDwUrl(taskSceneConf.getSyncDw(), taskSceneConf.getType(), taskSceneConf.getId());
        if (dwUrl != null) {
            HttpResponse<String> response = Requests.post(dwUrl, null, null, stdout);
            Requests.checkResponseStatus(response);
        }
    }

    private String buildDwUrl(String syncDw, String type, Long id) {
        String dwUrl = null;
        if ("true".equals(syncDw)) {
            switch (type) {
                case "model":
                    dwUrl = String.format("%s/dw/data/pushModelDatas?modelId=%s", collectPushDataEndpoint, id);
                    break;
                case "entity":
                    dwUrl = String.format("%s/dw/data/pushEntityDatas?entityId=%s", collectPushDataEndpoint, id);
                    break;
                default:
                    log.error("type is wrong: " + type);
            }
        }
        return dwUrl;
    }

    private String generateUid(Integer metricId, JSONObject labels) {
        return DigestUtils.md5DigestAsHex((metricId + "|" + JSONObject.toJSONString(labels)).getBytes());
    }

    private String generateId(Integer metricId, JSONObject labels, Long timestamp) {
        return DigestUtils.md5DigestAsHex((metricId + "|" + JSONObject.toJSONString(labels) + "|" + timestamp).getBytes());
    }

    private List<JSONObject> buildMetricDatas(Integer metricId, String result) throws Exception {
        List<JSONObject> metricDatas = JSONObject.parseArray(result, JSONObject.class);

        String metricMetaUrl = String.format("%s/metric/getMetricById?id=%s", pmdbEndpoint, metricId);
        HttpResponse<String> metricResponse = Requests.get(metricMetaUrl, null, null);
        if (metricResponse.statusCode() /100 == 2) {
            JSONObject body = JSONObject.parseObject(metricResponse.body());
            JSONObject metricMeta = body.getJSONObject("data");
            if (CollectionUtils.isEmpty(metricMeta)) {
                log.error("related metric is missing: " + metricId);
                throw new Exception("related metric is missing: " + metricId);
            }

            JSONObject metricLabels = metricMeta.getJSONObject("labels");
            metricLabels = metricLabels == null ? new JSONObject() : metricLabels;
            for (JSONObject metricData : metricDatas) {
                Long timestamp = metricData.getLong("timestamp");
                String tsStr = String.valueOf(timestamp);
                Preconditions.checkArgument(tsStr.length() == SECOND_TS_LENGTH || tsStr.length() == MILLISECOND_TS_LENGTH, "时间戳非法");
                if (tsStr.length() == SECOND_TS_LENGTH) {
                    timestamp = timestamp * 1000;
                }

                metricData.put("timestamp", timestamp);
                JSONObject instanceLabels = metricData.getJSONObject("labels");
                instanceLabels = instanceLabels == null ? new JSONObject() : instanceLabels;
                instanceLabels.putAll(metricLabels);
                metricData.put("labels", instanceLabels);
                metricData.put("metric_id", metricId);
                metricData.put("metric_name", metricMeta.getString("name"));
                metricData.put("type", metricMeta.getString("type"));
                metricData.put("uid", generateUid(metricId, instanceLabels));
                metricData.put("id", generateId(metricId, instanceLabels, metricData.getLong("timestamp")));
            }
        }
        return metricDatas;
    }

    private void pushPmdb(Integer metricId, List<JSONObject> metricDatas, String isPushQueue) throws Exception {
        String pushMetricDataUrl = String.format("%s/metric_instance/pushMetricDatas?metricId=%s&isInsertNewIns=true&isDeleteOldIns=false&isPushQueue=%s",
                pmdbEndpoint, metricId, isPushQueue);
        HttpResponse<String> response = Requests.post(pushMetricDataUrl, null, null, JSONObject.toJSONString(metricDatas));
        Requests.checkResponseStatus(response);
    }
}



