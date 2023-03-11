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

package com.netease.arctic.ams.server.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.server.config.ConfigFileProperties;
import com.netease.arctic.ams.server.mapper.CatalogMetadataMapper;
import com.netease.arctic.ams.server.service.IJDBCService;
import com.netease.arctic.utils.ConfigurationFileUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.ibatis.session.SqlSession;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CatalogMetadataService extends IJDBCService {

  public List<CatalogMeta> getCatalogs() {
    try (SqlSession sqlSession = getSqlSession(true)) {
      CatalogMetadataMapper catalogMetadataMapper =
              getMapper(sqlSession, CatalogMetadataMapper.class);
      return catalogMetadataMapper.getCatalogs();
    }
  }

  public Optional<CatalogMeta> getCatalog(String catalogName) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      CatalogMetadataMapper catalogMetadataMapper =
          getMapper(sqlSession, CatalogMetadataMapper.class);
      List<CatalogMeta> tmpMetadataList = catalogMetadataMapper.getCatalog(catalogName);
      if (CollectionUtils.isNotEmpty(tmpMetadataList)) {
        return Optional.of(tmpMetadataList.get(0));
      } else {
        return Optional.empty();
      }
    }
  }

  /**
   * check catalog exists
   *
   * @param catalogName
   * @return
   */
  public boolean catalogExist(String catalogName) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      CatalogMetadataMapper catalogMetadataMapper =
              getMapper(sqlSession, CatalogMetadataMapper.class);
      List<CatalogMeta> tmpMetadataList = catalogMetadataMapper.getCatalog(catalogName);
      if (CollectionUtils.isNotEmpty(tmpMetadataList)) {
        return true;
      } else {
        return false;
      }
    }
  }


  public void addCatalog(List<CatalogMeta> catalogMeta) {
    try (SqlSession sqlSession = getSqlSession(true)) {

      CatalogMetadataMapper catalogMetadataMapper =
          getMapper(sqlSession, CatalogMetadataMapper.class);
      for (CatalogMeta c : catalogMeta) {
        if (!getCatalog(c.catalogName).isPresent()) {
          catalogMetadataMapper.insertCatalog(c);
        }
      }
    }
  }

  /**
   * add one catalog
   *
   * @param catalogMeta
   */
  public void addCatalog(CatalogMeta catalogMeta) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      CatalogMetadataMapper catalogMetadataMapper =
              getMapper(sqlSession, CatalogMetadataMapper.class);
      catalogMetadataMapper.insertCatalog(catalogMeta);
    }
  }

  public CatalogMeta addCatalog(JSONObject catalog) throws IOException {
    CatalogMeta catalogMeta = new CatalogMeta();
    catalogMeta.catalogName = catalog.getString(ConfigFileProperties.CATALOG_NAME);
    catalogMeta.catalogType = catalog.getString(ConfigFileProperties.CATALOG_TYPE);

    if (catalog.containsKey(ConfigFileProperties.CATALOG_STORAGE_CONFIG)) {
      Map<String, String> storageConfig = new HashMap<>();
      JSONObject catalogStorageConfig = catalog.getJSONObject(ConfigFileProperties.CATALOG_STORAGE_CONFIG);

      if (catalogStorageConfig.containsKey(ConfigFileProperties.CATALOG_STORAGE_TYPE)) {
        storageConfig.put(
                CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
                catalogStorageConfig.getString(ConfigFileProperties.CATALOG_STORAGE_TYPE));
      }
      storageConfig.put(
              CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE,
              ConfigurationFileUtils.encodeXmlConfigurationFileWithBase64(
                      catalogStorageConfig.getString(ConfigFileProperties.CATALOG_CORE_SITE)));
      storageConfig.put(
              CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE,
              ConfigurationFileUtils.encodeXmlConfigurationFileWithBase64(
                      catalogStorageConfig.getString(ConfigFileProperties.CATALOG_HDFS_SITE)));
      storageConfig.put(
              CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE,
              ConfigurationFileUtils.encodeXmlConfigurationFileWithBase64(
                      catalogStorageConfig.getString(ConfigFileProperties.CATALOG_HIVE_SITE)));
      catalogMeta.storageConfigs = storageConfig;
    }

    if (catalog.containsKey(ConfigFileProperties.CATALOG_AUTH_CONFIG)) {
      Map<String, String> authConfig = new HashMap<>();
      JSONObject catalogAuthConfig = catalog.getJSONObject(ConfigFileProperties.CATALOG_AUTH_CONFIG);
      if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_AUTH_TYPE)) {
        authConfig.put(
                CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
                catalogAuthConfig.getString(ConfigFileProperties.CATALOG_AUTH_TYPE));
      }
      if (catalogAuthConfig.getString(ConfigFileProperties.CATALOG_AUTH_TYPE)
              .equalsIgnoreCase(CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE)) {
        if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_SIMPLE_HADOOP_USERNAME)) {
          authConfig.put(
                  CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
                  catalogAuthConfig.getString(ConfigFileProperties.CATALOG_SIMPLE_HADOOP_USERNAME));
        }
      } else if (catalogAuthConfig.getString(ConfigFileProperties.CATALOG_AUTH_TYPE)
              .equalsIgnoreCase(CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_KERBEROS)) {
        if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_KEYTAB)) {
          authConfig.put(
                  CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB,
                  ConfigurationFileUtils.encodeConfigurationFileWithBase64(
                          catalogAuthConfig.getString(ConfigFileProperties.CATALOG_KEYTAB)));
        }
        if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_KRB5)) {
          authConfig.put(
                  CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB5,
                  ConfigurationFileUtils.encodeConfigurationFileWithBase64(
                          catalogAuthConfig.getString(ConfigFileProperties.CATALOG_KRB5)));
        }
        if (catalogAuthConfig.containsKey(ConfigFileProperties.CATALOG_PRINCIPAL)) {
          authConfig.put(
                  CatalogMetaProperties.AUTH_CONFIGS_KEY_PRINCIPAL,
                  catalogAuthConfig.getString(ConfigFileProperties.CATALOG_PRINCIPAL));
        }
      }
      catalogMeta.authConfigs = authConfig;
    }

    if (catalog.containsKey(ConfigFileProperties.CATALOG_PROPERTIES)) {
      catalogMeta.catalogProperties = catalog.getObject(ConfigFileProperties.CATALOG_PROPERTIES, Map.class);
    }
    return catalogMeta;
  }

  public void deleteCatalog(String catalogName) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      CatalogMetadataMapper catalogMetadataMapper =
              getMapper(sqlSession, CatalogMetadataMapper.class);
      if (StringUtils.isNotEmpty(catalogName)) {
        catalogMetadataMapper.deleteCatalog(catalogName);
      }
    }
  }

  public void updateCatalog(CatalogMeta catalogMeta) {
    try (SqlSession sqlSession = getSqlSession(true)) {
      CatalogMetadataMapper catalogMetadataMapper =
              getMapper(sqlSession, CatalogMetadataMapper.class);
      catalogMetadataMapper.updateCatalog(catalogMeta);
    }
  }
}
