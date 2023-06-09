/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.flink.table.descriptors;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.connector.pulsar.sink.config.SinkConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

/**
 * It's used for converting Arctic log-store related properties in {@link com.netease.arctic.table.TableProperties}
 * to Flink official pulsar configuration.
 */
public class PulsarConfigurationConverter {

  private static final Logger LOG = LoggerFactory.getLogger(PulsarConfigurationConverter.class);

  /**
   * @param arcticProperties The key has been trimmed of Arctic prefix
   * @return Properties with Flink Pulsar source keys
   */
  public static SinkConfiguration toSinkConf(Properties arcticProperties) {
    Configuration conf = new Configuration();
    arcticProperties.stringPropertyNames().forEach(k -> {
      String v = (String) arcticProperties.get(k);
      conf.setString(k, v);
    });
    return new SinkConfiguration(conf);
  }

}
