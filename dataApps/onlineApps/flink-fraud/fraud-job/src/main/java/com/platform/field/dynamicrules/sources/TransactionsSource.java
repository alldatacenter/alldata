package com.platform.field.dynamicrules.sources;

import com.platform.field.config.Config;
import com.platform.field.dynamicrules.KafkaUtils;
import com.platform.field.dynamicrules.Transaction;
import com.platform.field.dynamicrules.functions.JsonDeserializer;
import com.platform.field.dynamicrules.functions.JsonGeneratorWrapper;
import com.platform.field.dynamicrules.functions.TimeStamper;
import com.platform.field.dynamicrules.functions.TransactionsGenerator;
import java.util.Properties;

import com.platform.field.config.Parameters;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer011;

public class TransactionsSource {

  public static SourceFunction<String> createTransactionsSource(Config config) {

    String sourceType = config.get(Parameters.TRANSACTIONS_SOURCE);
    TransactionsSource.Type transactionsSourceType =
        TransactionsSource.Type.valueOf(sourceType.toUpperCase());

    int transactionsPerSecond = config.get(Parameters.RECORDS_PER_SECOND);

    switch (transactionsSourceType) {
      case KAFKA:
        Properties kafkaProps = KafkaUtils.initConsumerProperties(config);
        String transactionsTopic = config.get(Parameters.DATA_TOPIC);
        FlinkKafkaConsumer011<String> kafkaConsumer =
            new FlinkKafkaConsumer011<>(transactionsTopic, new SimpleStringSchema(), kafkaProps);
        kafkaConsumer.setStartFromLatest();
        return kafkaConsumer;
      default:
        return new JsonGeneratorWrapper<>(new TransactionsGenerator(transactionsPerSecond));
    }
  }

  public static DataStream<Transaction> stringsStreamToTransactions(
      DataStream<String> transactionStrings) {
    return transactionStrings
        .flatMap(new JsonDeserializer<Transaction>(Transaction.class))
        .returns(Transaction.class)
        .flatMap(new TimeStamper<Transaction>())
        .returns(Transaction.class)
        .name("Transactions Deserialization");
  }

  public enum Type {
    GENERATOR("Transactions Source (generated locally)"),
    KAFKA("Transactions Source (Kafka)");

    private String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
