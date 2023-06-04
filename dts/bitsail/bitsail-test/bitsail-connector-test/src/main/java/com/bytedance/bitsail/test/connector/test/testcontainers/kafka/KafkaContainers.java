/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytedance.bitsail.test.connector.test.testcontainers.kafka;

import com.github.dockerjava.api.command.InspectContainerResponse;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Container;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

/**
 * Created 2022/7/26
 */
public class KafkaContainers extends KafkaContainer {
  private static final Logger LOG = LoggerFactory.getLogger(KafkaContainers.class);

  private String brokerAdvertisedListener;
  private String zkHost;

  public KafkaContainers(DockerImageName dockerImageName) {
    super(dockerImageName);
  }

  @Override
  protected String brokerAdvertisedListener(InspectContainerResponse containerInfo) {
    return String.format("%s:%s", containerInfo.getConfig().getHostName(), "9092");
  }

  @Override
  @SneakyThrows
  public void containerIsStarted(InspectContainerResponse containerInfo) {
    brokerAdvertisedListener = brokerAdvertisedListener(containerInfo);
    zkHost = brokerAdvertisedListener.replace("9092", "2181");
    String advertisedListeners = String.join(",",
        getBootstrapServers(),
        brokerAdvertisedListener);

    LOG.info("old brokerAdvertisedListener: {}", brokerAdvertisedListener);
    LOG.info("new brokerAdvertisedListener: {}", zkHost);
    LOG.info("entity-name: {}", getEnvMap().get("KAFKA_BROKER_ID"));
    LOG.info("BootstrapServers: {}", getBootstrapServers());
    LOG.info("advertised.listeners: {}", advertisedListeners);

    Container.ExecResult result = execInContainer(
        "kafka-configs",
        "--alter",
        "--zookeeper", zkHost,
        "--entity-type", "brokers",
        "--entity-name", getEnvMap().get("KAFKA_BROKER_ID"),
        "--add-config",
        "advertised.listeners=[" + advertisedListeners + "]"
    );
    if (result.getExitCode() != 0) {
      throw new IllegalStateException(result.toString());
    }
  }
}
