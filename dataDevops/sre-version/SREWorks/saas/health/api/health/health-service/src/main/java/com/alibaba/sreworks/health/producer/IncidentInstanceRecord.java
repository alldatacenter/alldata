package com.alibaba.sreworks.health.producer;

import com.alibaba.sreworks.health.domain.IncidentInstance;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * 异常实例消息
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 20:25
 */
public class IncidentInstanceRecord extends KafkaProducerRecord {

    @Override
    public ProducerRecord buildProducerRecord(String mqTopic, Object record) {
        IncidentInstance incidentInstance = (IncidentInstance) record;
        return new ProducerRecord<String, IncidentInstance>(mqTopic, incidentInstance.getTraceId(), incidentInstance);
    }
}
