package com.platform.field.dynamicrules.sinks;

import com.platform.field.config.Config;
import com.platform.field.dynamicrules.KafkaUtils;
import java.io.IOException;
import java.util.Properties;

import com.platform.field.config.Parameters;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.connectors.gcp.pubsub.PubSubSink;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaProducer011;

public class LatencySink {

  public static SinkFunction<String> createLatencySink(Config config) throws IOException {

    String latencySink = config.get(Parameters.LATENCY_SINK);
    LatencySink.Type latencySinkType = LatencySink.Type.valueOf(latencySink.toUpperCase());

    switch (latencySinkType) {
      case KAFKA:
        Properties kafkaProps = KafkaUtils.initProducerProperties(config);
        String latencyTopic = config.get(Parameters.LATENCY_TOPIC);
        return new FlinkKafkaProducer011<>(latencyTopic, new SimpleStringSchema(), kafkaProps);
      case PUBSUB:
        return PubSubSink.<String>newBuilder()
            .withSerializationSchema(new SimpleStringSchema())
            .withProjectName(config.get(Parameters.GCP_PROJECT_NAME))
            .withTopicName(config.get(Parameters.GCP_PUBSUB_LATENCY_SUBSCRIPTION))
            .build();
      case STDOUT:
        return new PrintSinkFunction<>(true);
      default:
        throw new IllegalArgumentException(
            "Source \"" + latencySinkType + "\" unknown. Known values are:" + Type.values());
    }
  }

  public enum Type {
    KAFKA("Latency Sink (Kafka)"),
    PUBSUB("Latency Sink (Pub/Sub)"),
    STDOUT("Latency Sink (Std. Out)");

    private String name;

    Type(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }
  }
}
