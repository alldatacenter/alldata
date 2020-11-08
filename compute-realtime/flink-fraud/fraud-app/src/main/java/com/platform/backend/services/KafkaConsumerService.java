package com.platform.backend.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.backend.entities.Rule;
import com.platform.backend.model.RulePayload;
import com.platform.backend.repositories.RuleRepository;
import java.io.IOException;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaConsumerService {

  private final SimpMessagingTemplate simpTemplate;
  private final RuleRepository ruleRepository;
  private final ObjectMapper mapper = new ObjectMapper();

  @Value("${web-socket.topic.alerts}")
  private String alertsWebSocketTopic;

  @Value("${web-socket.topic.latency}")
  private String latencyWebSocketTopic;

  @Autowired
  public KafkaConsumerService(SimpMessagingTemplate simpTemplate, RuleRepository ruleRepository) {
    this.simpTemplate = simpTemplate;
    this.ruleRepository = ruleRepository;
  }

  @KafkaListener(topics = "${kafka.topic.alerts}", groupId = "alerts")
  public void templateAlerts(@Payload String message) {
    log.debug("{}", message);
    simpTemplate.convertAndSend(alertsWebSocketTopic, message);
  }

  @KafkaListener(topics = "${kafka.topic.latency}", groupId = "latency")
  public void templateLatency(@Payload String message) {
    log.debug("{}", message);
    simpTemplate.convertAndSend(latencyWebSocketTopic, message);
  }

  @KafkaListener(topics = "${kafka.topic.current-rules}", groupId = "current-rules")
  public void templateCurrentFlinkRules(@Payload String message) throws IOException {
    log.info("{}", message);
    RulePayload payload = mapper.readValue(message, RulePayload.class);
    Integer payloadId = payload.getRuleId();
    Optional<Rule> existingRule = ruleRepository.findById(payloadId);
    if (!existingRule.isPresent()) {
      ruleRepository.save(new Rule(payloadId, mapper.writeValueAsString(payload)));
    }
  }
}
