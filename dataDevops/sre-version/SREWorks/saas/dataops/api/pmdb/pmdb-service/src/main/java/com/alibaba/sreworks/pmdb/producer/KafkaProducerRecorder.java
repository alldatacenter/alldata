package com.alibaba.sreworks.pmdb.producer;

import com.alibaba.fastjson.JSONObject;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 20:25
 */
public abstract class KafkaProducerRecorder {
    public abstract ProducerRecord<String, JSONObject> buildJsonValueRecord(String mqTopic, Object record);

    public abstract ProducerRecord<String, String> buildStringValueRecord(String mqTopic, Object record);
}
