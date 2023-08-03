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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

class KafkaRequestParamsUtilsTest {

    @Test
    void parsePropertiesFromRequestParams() {
        Map<String, String> requestParams =
                new ImmutableMap.Builder<String, String>()
                        .put(KafkaOptionRule.BOOTSTRAP_SERVERS.key(), "localhost:9092")
                        .put(
                                KafkaOptionRule.KAFKA_CONFIG.key(),
                                "{" + "security.protocol = SASL_PLAINTEXT" + "}")
                        .build();
        Properties properties =
                KafkaRequestParamsUtils.parsePropertiesFromRequestParams(requestParams);
        Assertions.assertEquals("SASL_PLAINTEXT", properties.getProperty("security.protocol"));
    }

    @Test
    void parsePropertiesFromRequestParamsBadCase() {
        Assertions.assertDoesNotThrow(
                () ->
                        KafkaRequestParamsUtils.parsePropertiesFromRequestParams(
                                new ImmutableMap.Builder<String, String>()
                                        .put(
                                                KafkaOptionRule.BOOTSTRAP_SERVERS.key(),
                                                "localhost:9092")
                                        .put(KafkaOptionRule.KAFKA_CONFIG.key(), "{}")
                                        .build()));

        Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> KafkaRequestParamsUtils.parsePropertiesFromRequestParams(new HashMap<>()));

        Assertions.assertDoesNotThrow(
                () ->
                        KafkaRequestParamsUtils.parsePropertiesFromRequestParams(
                                new ImmutableMap.Builder<String, String>()
                                        .put(
                                                KafkaOptionRule.BOOTSTRAP_SERVERS.key(),
                                                "localhost:9092")
                                        .put(KafkaOptionRule.KAFKA_CONFIG.key(), "")
                                        .build()));
    }
}
