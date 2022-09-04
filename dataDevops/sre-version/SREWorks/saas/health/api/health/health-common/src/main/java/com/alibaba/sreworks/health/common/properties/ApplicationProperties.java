package com.alibaba.sreworks.health.common.properties;


import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Data
public class ApplicationProperties {
    @Value("${spring.kafka.bootstrap-servers}")
    private String kafkaBootstrapServers;

    @Value("${spring.kafka.producer.acks}")
    private String kafkaProducerAcks;

    @Value("${spring.kafka.producer.key-serializer}")
    private String kafkaProducerKeySerializer;

    @Value("${spring.kafka.producer.value-serializer}")
    private String kafkaProducerValueSerializer;

    @Value("${spring.kafka.producer.batch-size}")
    private String kafkaProducerBatchSize;

    @Value("${spring.kafka.producer.linger.ms}")
    private String kafkaProducerLingerMS;

    @Value("${spring.kafka.producer.request.timeout.ms}")
    private String kafkaProducerRequestTimeoutMS;

    @Value("${spring.kafka.producer.delivery.timeout.ms}")
    private String kafkaProducerDeliveryTimeoutMS;

    @Value("${spring.app.protocol}")
    private String appProtocol;

    @Value("${spring.app.host}")
    private String appHost;

    @Value("${spring.app.port}")
    private Integer appPort;

    @Value("${spring.pmdb.protocol}")
    private String pmdbProtocol;

    @Value("${spring.pmdb.host}")
    private String pmdbHost;

    @Value("${spring.pmdb.port}")
    private Integer pmdbPort;

    @Value("${spring.dw.protocol}")
    private String dwProtocol;

    @Value("${spring.dw.host}")
    private String dwHost;

    @Value("${spring.dw.port}")
    private Integer dwPort;

    @Value("${spring.sreworks.job-master.protocol}")
    private String jobMasterProtocol;

    @Value("${spring.sreworks.job-master.host}")
    private String jobMasterHost;

    @Value("${spring.sreworks.job-master.port}")
    private Integer jobMasterPort;
}