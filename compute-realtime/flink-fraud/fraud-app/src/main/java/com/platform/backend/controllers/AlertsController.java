package com.platform.backend.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.platform.backend.datasource.Transaction;
import com.platform.backend.entities.Rule;
import com.platform.backend.exceptions.RuleNotFoundException;
import com.platform.backend.model.Alert;
import com.platform.backend.repositories.RuleRepository;
import com.platform.backend.services.KafkaTransactionsPusher;
import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class AlertsController {

  private final RuleRepository repository;
  private final KafkaTransactionsPusher transactionsPusher;
  private SimpMessagingTemplate simpSender;

  @Value("${web-socket.topic.alerts}")
  private String alertsWebSocketTopic;

  @Autowired
  public AlertsController(
      RuleRepository repository,
      KafkaTransactionsPusher transactionsPusher,
      SimpMessagingTemplate simpSender) {
    this.repository = repository;
    this.transactionsPusher = transactionsPusher;
    this.simpSender = simpSender;
  }

  ObjectMapper mapper = new ObjectMapper();

  @GetMapping("/rules/{id}/alert")
  Alert mockAlert(@PathVariable Integer id) throws JsonProcessingException {
    Rule rule = repository.findById(id).orElseThrow(() -> new RuleNotFoundException(id));
    Transaction triggeringEvent = transactionsPusher.getLastTransaction();
    String violatedRule = rule.getRulePayload();
    BigDecimal triggeringValue = triggeringEvent.getPaymentAmount().multiply(new BigDecimal(10));

    Alert alert = new Alert(rule.getId(), violatedRule, triggeringEvent, triggeringValue);

    String result = mapper.writeValueAsString(alert);

    simpSender.convertAndSend(alertsWebSocketTopic, result);

    return alert;
  }
}
