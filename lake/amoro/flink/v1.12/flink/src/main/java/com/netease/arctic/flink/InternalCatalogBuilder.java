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

package com.netease.arctic.flink;

import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.utils.ConfigurationFileUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.table.catalog.exceptions.CatalogException;
import org.apache.flink.util.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Build {@link ArcticCatalog}.
 */
public class InternalCatalogBuilder implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(InternalCatalogBuilder.class);

  private String metastoreUrl;
  private Map<String, String> properties = new HashMap<>(0);

  private ArcticCatalog createBaseArcticCatalog() {
    Preconditions.checkArgument(StringUtils.isNotBlank(metastoreUrl),
        "metastoreUrl can not be empty. e.g: thrift://127.0.0.1:port/catalogName");
    return CatalogLoader.load(metastoreUrl, properties);
  }

  public String getMetastoreUrl() {
    return metastoreUrl;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public InternalCatalogBuilder() {
  }

  public static InternalCatalogBuilder builder() {
    return new InternalCatalogBuilder();
  }

  public ArcticCatalog build() {
    return createBaseArcticCatalog();
  }

  public InternalCatalogBuilder metastoreUrl(String metastoreUrl) {
    Preconditions.checkArgument(StringUtils.isNotBlank(metastoreUrl),
        "metastore url can not be empty e.g: thrift://127.0.0.1:port/catalogName");
    this.metastoreUrl = metastoreUrl;
    return this;
  }

  public InternalCatalogBuilder properties(Map<String, String> properties) {
    Map<String, String> finalProperties = new HashMap<>();
    for (Map.Entry<String, String> property : properties.entrySet()) {
      String key = property.getKey();
      String value = property.getValue();
      switch (key) {
        case CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB_PATH:
          try {
            finalProperties.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB,
                ConfigurationFileUtil.encodeConfigurationFileWithBase64(value));
          } catch (IOException e) {
            LOG.error("encode keytab file failed", e);
            throw new CatalogException("encode keytab file failed", e);
          }
          break;
        case CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB_ENCODE:
          finalProperties.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB, value);
          break;
        case CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB_PATH:
          try {
            finalProperties.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB5,
                ConfigurationFileUtil.encodeConfigurationFileWithBase64(value));
          } catch (IOException e) {
            LOG.error("encode krb5 file failed", e);
            throw new CatalogException("encode krb5 file failed", e);
          }
          break;
        case CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB_ENCODE:
          finalProperties.put(CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB5, value);
          break;
        default:
          finalProperties.put(key, value);
          break;
      }
    }
    this.properties = finalProperties;
    return this;
  }
}
