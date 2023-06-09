/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store.druid;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.drill.shaded.guava.com.google.common.io.Resources;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

public class DruidStoragePluginConfigTest {

  @Test
  public void testDruidStoragePluginConfigSuccessfullyParsed()
      throws URISyntaxException, IOException {
    ObjectMapper mapper = new ObjectMapper();
    JsonNode storagePluginJson = mapper.readTree(new File(
        Resources.getResource("bootstrap-storage-plugins.json").toURI()));
    DruidStoragePluginConfig druidStoragePluginConfig =
        mapper.treeToValue(storagePluginJson.get("storage").get("druid"), DruidStoragePluginConfig.class);
    assertThat(druidStoragePluginConfig).isNotNull();
    assertThat(druidStoragePluginConfig.getBrokerAddress()).isEqualTo("http://localhost:8082");
    assertThat(druidStoragePluginConfig.getCoordinatorAddress()).isEqualTo("http://localhost:8081");
    assertThat(druidStoragePluginConfig.getAverageRowSizeBytes()).isEqualTo(200);
    assertThat(druidStoragePluginConfig.isEnabled()).isFalse();
  }

  @Test
  public void testDefaultRowSizeUsedWhenNotProvidedInConfig()
      throws JsonProcessingException {
    String druidConfigStr = "{\n" + "  \"storage\":{\n" + "    \"druid\" : {\n"
        + "      \"type\" : \"druid\",\n"
        + "      \"brokerAddress\" : \"http://localhost:8082\",\n"
        + "      \"coordinatorAddress\": \"http://localhost:8081\",\n"
        + "      \"enabled\" : false\n" + "    }\n" + "  }\n" + "}\n";
    ObjectMapper mapper = new ObjectMapper();
    JsonNode storagePluginJson = mapper.readTree(druidConfigStr);
    DruidStoragePluginConfig druidStoragePluginConfig =
        mapper.treeToValue(storagePluginJson.get("storage").get("druid"), DruidStoragePluginConfig.class);
    assertThat(druidStoragePluginConfig.getAverageRowSizeBytes()).isEqualTo(100);
  }
}
