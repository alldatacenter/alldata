package com.alibaba.sreworks.health.producer;

import com.alibaba.sreworks.health.common.properties.ApplicationProperties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Properties;

/**
 * @author: fangzong.lyj@alibaba-inc.com
 * @date: 2021/10/27 11:02
 */
@Service
public class HealthKafkaProducer implements InitializingBean {

    private Producer<String, String> producer;

    @Autowired
    ApplicationProperties applicationProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        initKafkaProducer();
    }

    private void initKafkaProducer() {
        Properties props = new Properties();
        props.put("bootstrap.servers", applicationProperties.getKafkaBootstrapServers());
        props.put("acks", applicationProperties.getKafkaProducerAcks());
        props.put("batch.size", applicationProperties.getKafkaProducerBatchSize());
        props.put("linger.ms", applicationProperties.getKafkaProducerLingerMS());
        props.put("request.timeout.ms", applicationProperties.getKafkaProducerRequestTimeoutMS());
        props.put("delivery.timeout.ms", applicationProperties.getKafkaProducerDeliveryTimeoutMS());
        props.put("key.serializer", applicationProperties.getKafkaProducerKeySerializer());
        props.put("value.serializer", applicationProperties.getKafkaProducerValueSerializer());

        producer = new KafkaProducer<>(props);
    }

    public void sendMsg(ProducerRecord record) {
        producer.send(record);
    }
}
