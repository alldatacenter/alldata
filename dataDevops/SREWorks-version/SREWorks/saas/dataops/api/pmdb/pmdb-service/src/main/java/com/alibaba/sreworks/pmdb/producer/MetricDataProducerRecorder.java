package com.alibaba.sreworks.pmdb.producer;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * 异常实例消息
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 20:25
 */
public class MetricDataProducerRecorder extends KafkaProducerRecorder {

    @Override
    public ProducerRecord<String, JSONObject> buildJsonValueRecord(String mqTopic, Object record) {
        JSONObject metricData = (JSONObject) record;
        return new ProducerRecord<>(mqTopic, metricData.getString("instanceId"), metricData);
    }

    @Override
    public ProducerRecord<String, String> buildStringValueRecord(String mqTopic, Object record) {
        return null;
    }
}
