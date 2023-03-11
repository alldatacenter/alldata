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

package com.netease.arctic.catalog;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.properties.TableFormat;
import org.apache.hadoop.conf.Configuration;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class CatalogTestHelpers {
  private static final String HADOOP_EMPTY_CONFIG_BASE64 =
      Base64.getEncoder().encodeToString("<configuration></configuration>".getBytes(StandardCharsets.UTF_8));

  public static CatalogMeta buildCatalogMeta(
      String catalogName, String type,
      Map<String, String> properties, TableFormat... tableFormats) {
    Map<String, String> storageConfig = new HashMap<>();
    storageConfig.put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS);
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE, HADOOP_EMPTY_CONFIG_BASE64);
    storageConfig.put(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE, HADOOP_EMPTY_CONFIG_BASE64);

    Map<String, String> authConfig = new HashMap<>();
    authConfig.put(
        CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE);
    authConfig.put(
        CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
        System.getProperty("user.name"));

    if (tableFormats != null && tableFormats.length > 0) {
      properties.put(
          CatalogMetaProperties.TABLE_FORMATS,
          Arrays.stream(tableFormats).map(TableFormat::name).collect(Collectors.joining(",")));
    }
    return new CatalogMeta(catalogName, type, storageConfig, authConfig, properties);
  }

  public static CatalogMeta buildHiveCatalogMeta(
      String catalogName, Map<String, String> properties,
      Configuration hiveConfiguration, TableFormat... tableFormats) {
    CatalogMeta meta = buildCatalogMeta(catalogName, CatalogMetaProperties.CATALOG_TYPE_HIVE, properties, tableFormats);
    meta.getStorageConfigs().put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE,
        encodeHadoopConfiguration(hiveConfiguration));
    return meta;
  }

  public static String encodeHadoopConfiguration(Configuration conf) {
    try {
      ByteArrayOutputStream out = new ByteArrayOutputStream();
      conf.writeXml(out);
      String configurationString = out.toString();
      return Base64.getEncoder().encodeToString(configurationString.getBytes(StandardCharsets.UTF_8));
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
