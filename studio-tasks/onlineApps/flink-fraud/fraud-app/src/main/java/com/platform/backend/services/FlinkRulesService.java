package com.platform.backend.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.backend.entities.Rule;
import com.platform.backend.model.RulePayload;
import com.platform.backend.model.RulePayload.RuleState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class FlinkRulesService {

  private KafkaTemplate<String, String> kafkaTemplate;

  @Value("${kafka.topic.rules}")
  private String topic;

  private final ObjectMapper mapper = new ObjectMapper();

  @Autowired
  public FlinkRulesService(KafkaTemplate<String, String> kafkaTemplate) {
    this.kafkaTemplate = kafkaTemplate;
  }

  public void addRule(Rule rule) {
    String payload = rule.getRulePayload();
    kafkaTemplate.send(topic, payload);
  }

  public void deleteRule(int ruleId) throws JsonProcessingException {
    RulePayload payload = new RulePayload();
    payload.setRuleId(ruleId);
    payload.setRuleState(RuleState.DELETE);
    String payloadJson = mapper.writeValueAsString(payload);
    kafkaTemplate.send(topic, payloadJson);
  }
}
