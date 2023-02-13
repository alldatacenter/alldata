package com.platform.field.config;

import java.util.Arrays;
import java.util.List;
import org.apache.flink.api.java.utils.ParameterTool;

public class Parameters {

  private final ParameterTool tool;

  public Parameters(ParameterTool tool) {
    this.tool = tool;
  }

  <T> T getOrDefault(Param<T> param) {
    if (!tool.has(param.getName())) {
      return param.getDefaultValue();
    }
    Object value;
    if (param.getType() == Integer.class) {
      value = tool.getInt(param.getName());
    } else if (param.getType() == Long.class) {
      value = tool.getLong(param.getName());
    } else if (param.getType() == Double.class) {
      value = tool.getDouble(param.getName());
    } else if (param.getType() == Boolean.class) {
      value = tool.getBoolean(param.getName());
    } else {
      value = tool.get(param.getName());
    }
    return param.getType().cast(value);
  }

  public static Parameters fromArgs(String[] args) {
    ParameterTool tool = ParameterTool.fromArgs(args);
    return new Parameters(tool);
  }

  // Kafka:
  public static final Param<String> KAFKA_HOST = Param.string("kafka-host", "master");
  public static final Param<Integer> KAFKA_PORT = Param.integer("kafka-port", 9092);

  public static final Param<String> DATA_TOPIC = Param.string("data-topic", "livetransactions");
  public static final Param<String> ALERTS_TOPIC = Param.string("alerts-topic", "alerts");
  public static final Param<String> RULES_TOPIC = Param.string("rules-topic", "rules");
  public static final Param<String> LATENCY_TOPIC = Param.string("latency-topic", "latency");
  public static final Param<String> RULES_EXPORT_TOPIC =
      Param.string("current-rules-topic", "current-rules");

  public static final Param<String> OFFSET = Param.string("offset", "latest");

  // GCP PubSub:
  public static final Param<String> GCP_PROJECT_NAME = Param.string("gcp-project", "da-fe-212612");
  public static final Param<String> GCP_PUBSUB_RULES_SUBSCRIPTION =
      Param.string("pubsub-rules", "rules-demo");
  public static final Param<String> GCP_PUBSUB_ALERTS_SUBSCRIPTION =
      Param.string("pubsub-alerts", "alerts-demo");
  public static final Param<String> GCP_PUBSUB_LATENCY_SUBSCRIPTION =
      Param.string("pubsub-latency", "latency-demo");
  public static final Param<String> GCP_PUBSUB_RULES_EXPORT_SUBSCRIPTION =
      Param.string("pubsub-rules-export", "current-rules-demo");

  // Socket
  public static final Param<Integer> SOCKET_PORT = Param.integer("pubsub-rules-export", 9999);

  // General:
  //    source/sink types: kafka / pubsub / socket
  public static final Param<String> RULES_SOURCE = Param.string("rules-source", "SOCKET");
  public static final Param<String> TRANSACTIONS_SOURCE = Param.string("data-source", "GENERATOR");
  public static final Param<String> ALERTS_SINK = Param.string("alerts-sink", "STDOUT");
  public static final Param<String> LATENCY_SINK = Param.string("latency-sink", "STDOUT");
  public static final Param<String> RULES_EXPORT_SINK = Param.string("rules-export-sink", "STDOUT");

  public static final Param<Integer> RECORDS_PER_SECOND = Param.integer("records-per-second", 2);

  public static final Param<Boolean> LOCAL_EXECUTION = Param.bool("local", false);

  public static final Param<Integer> SOURCE_PARALLELISM = Param.integer("source-parallelism", 2);

  public static final Param<Boolean> ENABLE_CHECKPOINTS = Param.bool("checkpoints", false);

  public static final Param<Integer> CHECKPOINT_INTERVAL =
      Param.integer("checkpoint-interval", 60_000_0);
  public static final Param<Integer> MIN_PAUSE_BETWEEN_CHECKPOINTS =
      Param.integer("min-pause-btwn-checkpoints", 10000);
  public static final Param<Integer> OUT_OF_ORDERNESS = Param.integer("out-of-orderdness", 500);

  //  List<Param> list = Arrays.asList(new String[]{"foo", "bar"});

  public static final List<Param<String>> STRING_PARAMS =
      Arrays.asList(
          KAFKA_HOST,
          DATA_TOPIC,
          ALERTS_TOPIC,
          RULES_TOPIC,
          LATENCY_TOPIC,
          RULES_EXPORT_TOPIC,
          OFFSET,
          GCP_PROJECT_NAME,
          GCP_PUBSUB_RULES_SUBSCRIPTION,
          GCP_PUBSUB_ALERTS_SUBSCRIPTION,
          GCP_PUBSUB_LATENCY_SUBSCRIPTION,
          GCP_PUBSUB_RULES_EXPORT_SUBSCRIPTION,
          RULES_SOURCE,
          TRANSACTIONS_SOURCE,
          ALERTS_SINK,
          LATENCY_SINK,
          RULES_EXPORT_SINK);

  public static final List<Param<Integer>> INT_PARAMS =
      Arrays.asList(
          KAFKA_PORT,
          SOCKET_PORT,
          RECORDS_PER_SECOND,
          SOURCE_PARALLELISM,
          CHECKPOINT_INTERVAL,
          MIN_PAUSE_BETWEEN_CHECKPOINTS,
          OUT_OF_ORDERNESS);

  public static final List<Param<Boolean>> BOOL_PARAMS =
      Arrays.asList(LOCAL_EXECUTION, ENABLE_CHECKPOINTS);
}
