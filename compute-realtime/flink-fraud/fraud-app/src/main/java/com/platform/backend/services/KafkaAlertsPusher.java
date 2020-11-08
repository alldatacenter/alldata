package com.platform.backend.services;

import com.platform.backend.model.Alert;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaAlertsPusher implements Consumer<Alert> {

  private KafkaTemplate<String, Object> kafkaTemplate;

  @Value("${kafka.topic.alerts}")
  private String topic;

  @Autowired
  public KafkaAlertsPusher(KafkaTemplate<String, Object> kafkaTemplateForJson) {
    this.kafkaTemplate = kafkaTemplateForJson;
  }

  @Override
  public void accept(Alert alert) {
    log.info("{}", alert);
    kafkaTemplate.send(topic, alert);
  }
}
