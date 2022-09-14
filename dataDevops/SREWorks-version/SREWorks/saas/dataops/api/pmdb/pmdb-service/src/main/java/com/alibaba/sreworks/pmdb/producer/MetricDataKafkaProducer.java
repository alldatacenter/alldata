package com.alibaba.sreworks.pmdb.producer;

import com.alibaba.fastjson.JSONObject;
import com.alibaba.sreworks.pmdb.common.properties.ApplicationProperties;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Properties;


@Service
public class MetricDataKafkaProducer implements InitializingBean {

    private Producer<String, JSONObject> producer;

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

    public void sendMsg(ProducerRecord<String, JSONObject> record) {
        producer.send(record);
    }
}
