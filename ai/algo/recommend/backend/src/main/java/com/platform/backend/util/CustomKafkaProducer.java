package com.platform.backend.util;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Properties;

public class CustomKafkaProducer {

    private static String RATING_TOPIC = "rating";
    private static KafkaProducer<String, String> producer;
    static {
        Properties properties = new Properties();
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,  Property.getStrValue("kafka.bootstrap.servers"));
        producer = new KafkaProducer(properties);
    }

    public static void produce(String msg) {
        ProducerRecord<String, String> record = new ProducerRecord<>(RATING_TOPIC, msg);
        producer.send(record);
    }
}
