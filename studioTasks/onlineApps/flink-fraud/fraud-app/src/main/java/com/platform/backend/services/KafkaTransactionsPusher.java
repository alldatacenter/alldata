package com.platform.backend.services;

import com.platform.backend.datasource.Transaction;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaTransactionsPusher implements Consumer<Transaction> {

  private KafkaTemplate<String, Object> kafkaTemplate;
  private Transaction lastTransaction;

  @Value("${kafka.topic.transactions}")
  private String topic;

  @Autowired
  public KafkaTransactionsPusher(KafkaTemplate<String, Object> kafkaTemplateForJson) {
    this.kafkaTemplate = kafkaTemplateForJson;
  }

  @Override
  public void accept(Transaction transaction) {
    lastTransaction = transaction;
    log.debug("{}", transaction);
    kafkaTemplate.send(topic, transaction);
  }

  public Transaction getLastTransaction() {
    return lastTransaction;
  }
}
