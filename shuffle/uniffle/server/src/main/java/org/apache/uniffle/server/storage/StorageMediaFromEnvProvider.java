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

package org.apache.uniffle.server.storage;

import java.io.File;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.config.RssConf;
import org.apache.uniffle.common.storage.StorageMedia;
import org.apache.uniffle.server.ShuffleServerConf;
import org.apache.uniffle.storage.common.StorageMediaProvider;

public class StorageMediaFromEnvProvider implements StorageMediaProvider {
  private static Logger logger = LoggerFactory.getLogger(StorageMediaFromEnvProvider.class);
  private Map<String, StorageMedia> localStorageTypes = Maps.newHashMap();

  /**
   * Returns current storage type for baseDir.
   *
   * @param baseDir the storage dir to check for
   * @return LocalStorageType if valid or queryable. Unknown would be returned if it cannot be determined.
   */
  @Override
  public StorageMedia getStorageMediaFor(String baseDir) {
    File f = new File(baseDir);
    while (f != null) {
      if (localStorageTypes.containsKey(f.getPath())) {
        return localStorageTypes.get(f.getPath());
      }
      f = f.getParentFile();
    }
    return StorageMedia.UNKNOWN;
  }

  @Override
  public void init(RssConf conf) {
    String envKey = conf.get(ShuffleServerConf.STORAGE_MEDIA_PROVIDER_ENV_KEY);
    String jsonSource = "{}";
    if (envKey != null && System.getenv(envKey) != null) {
      jsonSource = System.getenv(envKey);
    }
    ObjectMapper om = new ObjectMapper();
    try {
      Map<String, String> mappings = om.readValue(jsonSource, Map.class);
      localStorageTypes.clear();
      for (Map.Entry<String, String> entry : mappings.entrySet()) {
        String basePath = entry.getKey();
        String storageType = entry.getValue();
        try {
          StorageMedia type = StorageMedia.valueOf(storageType.trim().toUpperCase());
          localStorageTypes.put(basePath, type);
        } catch (IllegalArgumentException i) {
          logger.warn("cannot get storage type from {}, ignoring", storageType);
        }
      }
    } catch (JsonProcessingException e) {
      logger.warn("parse json from env failed with exception", e);
    }
  }
}
