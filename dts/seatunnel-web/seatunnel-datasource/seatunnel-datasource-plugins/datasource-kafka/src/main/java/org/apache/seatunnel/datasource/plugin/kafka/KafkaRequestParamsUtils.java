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

package org.apache.seatunnel.datasource.plugin.kafka;

import org.apache.seatunnel.shade.com.typesafe.config.Config;
import org.apache.seatunnel.shade.com.typesafe.config.ConfigFactory;

import org.apache.kafka.clients.admin.AdminClientConfig;

import java.util.Map;
import java.util.Properties;

import static com.google.common.base.Preconditions.checkArgument;

public class KafkaRequestParamsUtils {

    public static Properties parsePropertiesFromRequestParams(Map<String, String> requestParams) {
        checkArgument(
                requestParams.containsKey(KafkaOptionRule.BOOTSTRAP_SERVERS.key()),
                String.format(
                        "Missing %s in requestParams", KafkaOptionRule.BOOTSTRAP_SERVERS.key()));
        final Properties properties = new Properties();
        properties.put(
                AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG,
                requestParams.get(KafkaOptionRule.BOOTSTRAP_SERVERS.key()));
        if (requestParams.containsKey(KafkaOptionRule.KAFKA_CONFIG.key())) {
            Config configObject =
                    ConfigFactory.parseString(
                            requestParams.get(KafkaOptionRule.KAFKA_CONFIG.key()));
            configObject
                    .entrySet()
                    .forEach(
                            entry -> {
                                properties.put(
                                        entry.getKey(), entry.getValue().unwrapped().toString());
                            });
        }
        return properties;
    }
}
