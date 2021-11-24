package com.platform.field.dynamicrules;

import com.platform.field.config.Config;
import com.platform.field.config.Parameters;

import java.util.Properties;

public class KafkaUtils {

  public static Properties initConsumerProperties(Config config) {
    Properties kafkaProps = initProperties(config);
    String offset = config.get(Parameters.OFFSET);
    kafkaProps.setProperty("auto.offset.reset", offset);
    return kafkaProps;
  }

  public static Properties initProducerProperties(Config params) {
    return initProperties(params);
  }

  private static Properties initProperties(Config config) {
    Properties kafkaProps = new Properties();
    String kafkaHost = config.get(Parameters.KAFKA_HOST);
    int kafkaPort = config.get(Parameters.KAFKA_PORT);
    String servers = String.format("%s:%s", kafkaHost, kafkaPort);
    kafkaProps.setProperty("bootstrap.servers", servers);
    return kafkaProps;
  }
}
