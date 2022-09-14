package com.alibaba.sreworks.health.producer;

import com.alibaba.sreworks.health.domain.AlertInstance;
import org.apache.kafka.clients.producer.ProducerRecord;

/**
 * 异常实例消息
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 20:25
 */
public class AlertInstanceRecord extends KafkaProducerRecord {

    @Override
    public ProducerRecord buildProducerRecord(String mqTopic, Object record) {
        AlertInstance alertInstance = (AlertInstance) record;
        return new ProducerRecord<String, AlertInstance>(mqTopic, alertInstance);
    }
}
