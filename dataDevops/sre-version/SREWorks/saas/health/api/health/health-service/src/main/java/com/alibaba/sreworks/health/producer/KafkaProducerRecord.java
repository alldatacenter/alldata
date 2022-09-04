package com.alibaba.sreworks.health.producer;

import org.apache.kafka.clients.producer.ProducerRecord;

/**
 *
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 20:25
 */
public abstract class KafkaProducerRecord {
    public abstract ProducerRecord buildProducerRecord(String mqTopic, Object record);
}
