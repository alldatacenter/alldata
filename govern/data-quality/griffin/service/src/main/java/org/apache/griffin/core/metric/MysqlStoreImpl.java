package org.apache.griffin.core.metric;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.griffin.core.mapper.MetricsMapper;
import org.apache.griffin.core.metric.model.MetricValue;
import org.apache.griffin.core.metric.model.MetricValueJson;
import org.apache.griffin.core.metric.model.MysqlMetrics;
import org.apache.griffin.core.util.JsonUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Change the original ES query Metric data to get data from Mysql
 *
 * @author AllDataDC
 * @date 2022/5/19
 */
@Component
public class MysqlStoreImpl implements MetricStore {

    private static final Logger logger = LoggerFactory.getLogger(MysqlStoreImpl.class);

    @Autowired
    MetricsMapper metricsMapper;

    private ObjectMapper mapper = new ObjectMapper();

    @Override
    public List<MetricValue> getMetricValues(String metricName, int from, int size, long tmst) {
        List<MetricValueJson> metricValueJsons = metricsMapper.selectMetrics(metricName);
        List<MetricValue> metricValues = toMetriValue(metricValueJsons);
        return metricValues;
    }

    /**
     * {
     * "job_name" : "metricName",
     * "tmst" : 1509599811123,
     * "applicationId" : "app_1",
     * "value" : {
     * "matchedFraction" : 1.0,
     * "miss" : 11,
     * "total" : 125000,
     * "matched" : 124989
     * }
     * "metadata": {"applicationId": "app_1"}
     * }
     *
     * @param metricValues
     * @return
     */

    @Override
    public ResponseEntity<?> addMetricValues(List<MetricValue> metricValues) throws IOException {
        List<Integer> insertResult = new ArrayList<>();
        ResponseEntity<?> responseEntity;
        try {
            for (int i = 0; i < metricValues.size(); i++) {
                MetricValue metricValue = metricValues.get(i);
                String metricJson = JsonUtil.toJson(metricValue.getValue());
                JSONObject metricJObject = JSON.parseObject(metricJson);
                String jobName = metricValue.getName();
                String metadataJson = JsonUtil.toJson(metricValue.getMetadata());
                JSONObject metadataJObject = JSON.parseObject(metadataJson);
                String tmst = String.valueOf(metricValue.getTmst());
                String applicationId = metadataJObject.get("applicationId") == null ? "" :
                        metadataJObject.get("applicationId").toString();
                MysqlMetrics metric = new MysqlMetrics(tmst, applicationId, jobName, metadataJObject, metricJObject);
                insertResult.add(metricsMapper.insertMetrics(metric));
            }
        } catch (Exception e) {
            logger.error("addMetricValues error..\n" + e.getMessage());
            return new ResponseEntity<>(insertResult, new HttpHeaders(), HttpStatus.EXPECTATION_FAILED);
        }
        logger.error("addMetricValues success..\n" + insertResult);
        responseEntity = new ResponseEntity<>(insertResult, new HttpHeaders(), HttpStatus.OK);
        return responseEntity;
    }


    private List<MetricValue> toMetriValue(List<MetricValueJson> from) {
        List<MetricValue> metricValues  = new ArrayList<>();
        from.stream().forEach(json -> {
            HashMap metaMap = new Gson().fromJson(json.getMetaJson(), HashMap.class);
            HashMap valueMap = new Gson().fromJson(json.getValueJson(), HashMap.class);
            MetricValue metricValue = new MetricValue(json.getName(), json.getTmst(), metaMap, valueMap);
            metricValues.add(metricValue);
        });
        System.out.println(metricValues);
        return metricValues;
    }


    @Override
    public ResponseEntity<?> deleteMetricValues(String jobName) {
        List<Integer> insertResult = new ArrayList<>();
        ResponseEntity<?> responseEntity;
        try {
            insertResult.add(metricsMapper.deleteMetricsByJobName(jobName));
        } catch (Exception e) {
            logger.error("addMetricValues error..\n" + e.getMessage());
            return new ResponseEntity<>(insertResult, new HttpHeaders(), HttpStatus.EXPECTATION_FAILED);
        }
        responseEntity = new ResponseEntity<>(insertResult, new HttpHeaders(), HttpStatus.OK);
        return responseEntity;
    }


    @Override
    public MetricValue getMetric(String applicationId) {
        return new MetricValue();
    }
}
