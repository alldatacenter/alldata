package com.platform.field.config;

import static com.platform.field.config.Parameters.KAFKA_HOST;
import static com.platform.field.config.Parameters.KAFKA_PORT;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ConfigTest {

  @Test
  public void testParameters() {
    String[] args = new String[] {"--kafka-host", "host-from-args"};
    Parameters parameters = Parameters.fromArgs(args);
    Config config = Config.fromParameters(parameters);

    final String kafkaHost = config.get(KAFKA_HOST);
    assertEquals("Wrong config parameter retrived", "host-from-args", kafkaHost);
  }

  @Test
  public void testParameterWithDefaults() {
    String[] args = new String[] {};
    Parameters parameters = Parameters.fromArgs(args);
    Config config = Config.fromParameters(parameters);

    final Integer kafkaPort = config.get(KAFKA_PORT);
    assertEquals("Wrong config parameter retrived", new Integer(9092), kafkaPort);
  }
}
