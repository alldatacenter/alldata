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

package org.apache.uniffle.server;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.apache.uniffle.common.storage.StorageMedia;
import org.apache.uniffle.server.storage.StorageMediaFromEnvProvider;
import org.apache.uniffle.storage.common.StorageMediaProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static uk.org.webcompere.systemstubs.SystemStubs.withEnvironmentVariables;

public class LocalSingleStorageTypeFromEnvProviderTest {
  private static final String STORAGE_TYPE_ENV_KEY = "RSS_LOCAL_STORAGE_TYPES";

  private ShuffleServerConf rssConf;
  private StorageMediaProvider provider;

  @BeforeEach
  void setupRssServerConfig() {
    rssConf = new ShuffleServerConf();
    rssConf.set(ShuffleServerConf.STORAGE_MEDIA_PROVIDER_ENV_KEY, STORAGE_TYPE_ENV_KEY);
    provider = new StorageMediaFromEnvProvider();
  }

  @Test
  public void testJsonSourceParse() throws Exception {
    String emptyJsonSource = "";
    withEnvironmentVariables(STORAGE_TYPE_ENV_KEY, emptyJsonSource).execute(() -> {
      // invalid json source should not throw exceptions
      provider.init(rssConf);
      assertEquals(StorageMedia.UNKNOWN, provider.getStorageMediaFor("/data01"));
    });
    emptyJsonSource = "{}";
    withEnvironmentVariables(STORAGE_TYPE_ENV_KEY, emptyJsonSource).execute(() -> {
      provider.init(rssConf);
      assertEquals(StorageMedia.UNKNOWN, provider.getStorageMediaFor("/data01"));
    });

    String storageTypeJson = "{\"/data01\": \"SSD\"}";
    withEnvironmentVariables(STORAGE_TYPE_ENV_KEY, storageTypeJson).execute(() -> {
      provider.init(rssConf);
      assertEquals(StorageMedia.SSD, provider.getStorageMediaFor("/data01"));
    });
  }

  @Test
  public void testMultipleMountPoints() throws Exception {
    Map<String, String> storageTypes = Maps.newHashMap();
    storageTypes.put("/data01", "ssd");
    storageTypes.put("/data02", "hdd");
    storageTypes.put("/data03", "SSD");
    ObjectMapper om = new ObjectMapper();
    String jsonSource;
    try {
      jsonSource = om.writeValueAsString(storageTypes);
    } catch (JsonProcessingException e) {
      throw new RuntimeException(e);
    }
    withEnvironmentVariables(STORAGE_TYPE_ENV_KEY, jsonSource).execute(() -> provider.init(rssConf));

    assertEquals(StorageMedia.HDD, provider.getStorageMediaFor("/data02"));
    assertEquals(StorageMedia.SSD, provider.getStorageMediaFor("/data01"));
    assertEquals(StorageMedia.SSD, provider.getStorageMediaFor("/data03"));
    assertEquals(StorageMedia.UNKNOWN, provider.getStorageMediaFor("/data0"));
    assertEquals(StorageMedia.UNKNOWN, provider.getStorageMediaFor("/Path/not/existed"));
    assertEquals(StorageMedia.HDD, provider.getStorageMediaFor("/data02/abc/1234"));
    assertEquals(StorageMedia.SSD, provider.getStorageMediaFor("/data01/spark_shuffle_data/111"));
  }
}
