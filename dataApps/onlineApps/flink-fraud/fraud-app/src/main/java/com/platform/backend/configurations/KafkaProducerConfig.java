package com.platform.backend.configurations;

import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Configuration
public class KafkaProducerConfig {

  @Value("${kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Bean
  public Map<String, Object> producerConfigsJson() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    return props;
  }

  @Bean
  public Map<String, Object> producerConfigsString() {
    Map<String, Object> props = new HashMap<>();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    return props;
  }

  // Transactions
  @Bean
  public ProducerFactory<String, Object> producerFactoryForJson() {
    return new DefaultKafkaProducerFactory<>(producerConfigsJson());
  }

  @Bean
  public KafkaTemplate<String, Object> kafkaTemplateForJson() {
    return new KafkaTemplate<>(producerFactoryForJson());
  }

  // Strings
  @Bean
  public ProducerFactory<String, String> producerFactoryForString() {
    return new DefaultKafkaProducerFactory<>(producerConfigsString());
  }

  @Bean
  public KafkaTemplate<String, String> kafkaTemplateForString() {
    return new KafkaTemplate<>(producerFactoryForString());
  }
}
