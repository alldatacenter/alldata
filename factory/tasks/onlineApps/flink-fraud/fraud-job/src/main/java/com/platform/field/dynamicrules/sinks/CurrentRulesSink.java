package com.platform.field.dynamicrules.sinks;

import com.platform.field.config.Config;
import com.platform.field.dynamicrules.KafkaUtils;
import com.platform.field.dynamicrules.Rule;
import com.platform.field.dynamicrules.functions.JsonSerializer;
import java.io.IOException;
import java.util.Properties;

import com.platform.field.config.Parameters;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.gcp.pubsub.PubSubSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011;

public class CurrentRulesSink {

  public static SinkFunction<String> createRulesSink(Config config) throws IOException {

    String sinkType = config.get(Parameters.RULES_EXPORT_SINK);
    CurrentRulesSink.Type currentRulesSinkType =
        CurrentRulesSink.Type.valueOf(sinkType.toUpperCase());

    switch (currentRulesSinkType) {
      case KAFKA:
        Properties kafkaProps = KafkaUtils.initProducerProperties(config);
        String alertsTopic = config.get(Parameters.RULES_EXPORT_TOPIC);
        return new FlinkKafkaProducer011<>(alertsTopic, new SimpleStringSchema(), kafkaProps);
      case PUBSUB:
        return PubSubSink.<String>newBuilder()
            .withSerializationSchema(new SimpleStringSchema())
            .withProjectName(config.get(Parameters.GCP_PROJECT_NAME))
            .withTopicName(config.get(Parameters.GCP_PUBSUB_RULES_SUBSCRIPTION))
            .build();
      case STDOUT:
        return new PrintSinkFunction<>(true);
      default:
        throw new IllegalArgumentException(
            "Source \"" + currentRulesSinkType + "\" unknown. Known values are:" + Type.values());
    }
  }

  public static DataStream<String> rulesStreamToJson(DataStream<Rule> alerts) {
    return alerts.flatMap(new JsonSerializer<>(Rule.class)).name("Rules Deserialization");
  }

  public enum Type {
    KAFKA("Current Rules Sink (Kafka)"),
    PUBSUB("Current Rules Sink (Pub/Sub)"),
    STDOUT("Current Rules Sink (Std. Out)");

    private String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
