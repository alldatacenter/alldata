package com.platform.field.dynamicrules.sinks;

import com.platform.field.config.Config;
import com.platform.field.dynamicrules.Alert;
import com.platform.field.dynamicrules.KafkaUtils;
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

public class AlertsSink {

  public static SinkFunction<String> createAlertsSink(Config config) throws IOException {

    String sinkType = config.get(Parameters.ALERTS_SINK);
    AlertsSink.Type alertsSinkType = AlertsSink.Type.valueOf(sinkType.toUpperCase());

    switch (alertsSinkType) {
      case KAFKA:
        Properties kafkaProps = KafkaUtils.initProducerProperties(config);
        String alertsTopic = config.get(Parameters.ALERTS_TOPIC);
        return new FlinkKafkaProducer011<>(alertsTopic, new SimpleStringSchema(), kafkaProps);
      case PUBSUB:
        return PubSubSink.<String>newBuilder()
            .withSerializationSchema(new SimpleStringSchema())
            .withProjectName(config.get(Parameters.GCP_PROJECT_NAME))
            .withTopicName(config.get(Parameters.GCP_PUBSUB_ALERTS_SUBSCRIPTION))
            .build();
      case STDOUT:
        return new PrintSinkFunction<>(true);
      default:
        throw new IllegalArgumentException(
            "Source \"" + alertsSinkType + "\" unknown. Known values are:" + Type.values());
    }
  }

  public static DataStream<String> alertsStreamToJson(DataStream<Alert> alerts) {
    return alerts.flatMap(new JsonSerializer<>(Alert.class)).name("Alerts Deserialization");
  }

  public enum Type {
    KAFKA("Alerts Sink (Kafka)"),
    PUBSUB("Alerts Sink (Pub/Sub)"),
    STDOUT("Alerts Sink (Std. Out)");

    private String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
