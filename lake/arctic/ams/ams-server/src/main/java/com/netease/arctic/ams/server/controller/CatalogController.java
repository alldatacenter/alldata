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

package com.netease.arctic.ams.server.controller;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.properties.TableFormat;
import com.netease.arctic.ams.server.config.ConfigFileProperties;
import com.netease.arctic.ams.server.controller.response.ErrorResponse;
import com.netease.arctic.ams.server.controller.response.OkResponse;
import com.netease.arctic.ams.server.model.CatalogRegisterInfo;
import com.netease.arctic.ams.server.model.CatalogSettingInfo;
import com.netease.arctic.ams.server.model.CatalogSettingInfo.ConfigFileItem;
import com.netease.arctic.ams.server.service.IMetaService;
import com.netease.arctic.ams.server.service.ServiceContainer;
import com.netease.arctic.ams.server.service.impl.CatalogMetadataService;
import com.netease.arctic.ams.server.service.impl.PlatformFileInfoService;
import com.netease.arctic.ams.server.utils.CatalogUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpCode;
import org.apache.commons.lang.StringUtils;
import org.apache.iceberg.CatalogProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB5;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.AUTH_CONFIGS_KEY_PRINCIPAL;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_KERBEROS;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_AMS;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_CUSTOM;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HADOOP;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.CATALOG_TYPE_HIVE;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.KEY_WAREHOUSE;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS;
import static com.netease.arctic.ams.api.properties.CatalogMetaProperties.TABLE_FORMATS;

public class CatalogController extends RestBaseController {
  private static final Logger LOG = LoggerFactory.getLogger(CatalogController.class);
  private static final IMetaService iMetaService = ServiceContainer.getMetaService();
  private static final CatalogMetadataService catalogMetadataService = ServiceContainer.getCatalogMetadataService();
  private static final PlatformFileInfoService platformFileInfoService = ServiceContainer.getPlatformFileInfoService();

  private static final String CONFIG_TYPE_STORAGE = "storage-config";
  private static final String CONFIG_TYPE_AUTH = "auth-config";
  // <configuration></configuration>  encoded with base64
  private static final String EMPTY_XML_BASE64 = "PGNvbmZpZ3VyYXRpb24+PC9jb25maWd1cmF0aW9uPg==";

  private static final Map<String, List<String>> CATALOG_REQUIRED_PROPERTIES;

  static {
    CATALOG_REQUIRED_PROPERTIES = Maps.newHashMap();
    CATALOG_REQUIRED_PROPERTIES.put(CATALOG_TYPE_AMS, Lists.newArrayList(KEY_WAREHOUSE));
    CATALOG_REQUIRED_PROPERTIES.put(CATALOG_TYPE_HADOOP, Lists.newArrayList(CatalogProperties.WAREHOUSE_LOCATION));
    CATALOG_REQUIRED_PROPERTIES.put(CATALOG_TYPE_CUSTOM, Lists.newArrayList(CatalogProperties.CATALOG_IMPL));
  }

  /**
   * Get catalog Type list
   */
  public static void getCatalogTypeList(Context ctx) {

    List<ImmutableMap<String, String>> catalogTypes = new ArrayList<>();
    String valueKey = "value";
    String displayKey = "display";
    catalogTypes.add(ImmutableMap.of(valueKey, CATALOG_TYPE_AMS, displayKey, "Arctic Metastore"));
    catalogTypes.add(ImmutableMap.of(valueKey, CATALOG_TYPE_HIVE, displayKey, "Hive Metastore"));
    catalogTypes.add(ImmutableMap.of(valueKey, CATALOG_TYPE_HADOOP, displayKey, "Hadoop"));
    catalogTypes.add(ImmutableMap.of(valueKey, CATALOG_TYPE_CUSTOM, displayKey, "Custom"));
    ctx.json(OkResponse.of(catalogTypes));
  }

  /**
   * Convert server auth config to metaAuthConfig
   */
  private static Map<String, String> authConvertFromServerToMeta(
      Map<String, String> serverAuthConfig,
      CatalogMeta oldCatalogMeta) {
    Map<String, String> metaAuthConfig = new HashMap<>();
    String authType = serverAuthConfig.getOrDefault(AUTH_CONFIGS_KEY_TYPE, "SIMPLE").toLowerCase();
    metaAuthConfig.put(AUTH_CONFIGS_KEY_TYPE, authType);
    Map<String, String> oldAuthConfig = new HashMap<>();
    if (oldCatalogMeta != null) {
      oldAuthConfig = oldCatalogMeta.getAuthConfigs();
    }

    if (authType.equals(AUTH_CONFIGS_VALUE_TYPE_SIMPLE)) {
      metaAuthConfig.put(
          AUTH_CONFIGS_KEY_HADOOP_USERNAME,
          serverAuthConfig.get(AUTH_CONFIGS_KEY_HADOOP_USERNAME));
    } else if (authType.equals(AUTH_CONFIGS_VALUE_TYPE_KERBEROS)) {
      String keytabFileId = serverAuthConfig.get(AUTH_CONFIGS_KEY_KEYTAB);
      if (!StringUtils.isEmpty(keytabFileId)) {
        String keytabB64 = platformFileInfoService.getFileContentB64ById(Integer.valueOf(keytabFileId));
        metaAuthConfig.put(AUTH_CONFIGS_KEY_KEYTAB, keytabB64);
      } else {
        metaAuthConfig.put(
            AUTH_CONFIGS_KEY_KEYTAB,
            oldAuthConfig.get(AUTH_CONFIGS_KEY_KEYTAB));
      }

      String krbFileId = serverAuthConfig.get(AUTH_CONFIGS_KEY_KRB5);
      if (!StringUtils.isEmpty(krbFileId)) {
        String krbB64 = platformFileInfoService.getFileContentB64ById(Integer.valueOf(krbFileId));
        metaAuthConfig.put(AUTH_CONFIGS_KEY_KRB5, krbB64);
      } else {
        metaAuthConfig.put(
            AUTH_CONFIGS_KEY_KRB5,
            oldAuthConfig.get(AUTH_CONFIGS_KEY_KRB5));
      }
      metaAuthConfig.put(
          AUTH_CONFIGS_KEY_PRINCIPAL,
          serverAuthConfig.get(AUTH_CONFIGS_KEY_PRINCIPAL));
    }
    return metaAuthConfig;
  }

  /**
   * Convert meta auth config to server auth config DTO
   */
  private static Map<String, Object> authConvertFromMetaToServer(
      String catalogName,
      Map<String, String> metaAuthConfig) {
    Map<String, Object> serverAuthConfig = new HashMap<>();
    serverAuthConfig.put(
        AUTH_CONFIGS_KEY_TYPE,
        metaAuthConfig.getOrDefault(
            AUTH_CONFIGS_KEY_TYPE,
            "simple").toUpperCase());
    serverAuthConfig.put(
        AUTH_CONFIGS_KEY_HADOOP_USERNAME,
        metaAuthConfig.get(AUTH_CONFIGS_KEY_HADOOP_USERNAME));

    serverAuthConfig.put(
        AUTH_CONFIGS_KEY_PRINCIPAL,
        metaAuthConfig.get(AUTH_CONFIGS_KEY_PRINCIPAL));

    serverAuthConfig.put(AUTH_CONFIGS_KEY_KEYTAB, new ConfigFileItem(
        catalogName + ".keytab",
        constructCatalogConfigFileUrl(catalogName, CONFIG_TYPE_AUTH,
            AUTH_CONFIGS_KEY_KEYTAB.replace("\\.", "-"))));

    serverAuthConfig.put(AUTH_CONFIGS_KEY_KRB5, new ConfigFileItem(
        "krb5.conf",
        constructCatalogConfigFileUrl(catalogName, CONFIG_TYPE_AUTH,
            AUTH_CONFIGS_KEY_KRB5.replace("\\.", "-"))));
    return serverAuthConfig;
  }

  private static Map<String, Object> storageConvertFromMetaToServer(String catalogName, Map<String, String> config) {
    Map<String, Object> storageConfig = new HashMap<>();
    storageConfig.put(STORAGE_CONFIGS_KEY_CORE_SITE, new ConfigFileItem(
        ConfigFileProperties.CATALOG_CORE_SITE + ".xml",
        constructCatalogConfigFileUrl(catalogName, CONFIG_TYPE_STORAGE,
            STORAGE_CONFIGS_KEY_CORE_SITE.replace("\\.", "-"))));

    storageConfig.put(STORAGE_CONFIGS_KEY_HDFS_SITE, new ConfigFileItem(
        ConfigFileProperties.CATALOG_HDFS_SITE + ".xml",
        constructCatalogConfigFileUrl(catalogName, CONFIG_TYPE_STORAGE,
            STORAGE_CONFIGS_KEY_HDFS_SITE.replace("\\.", "-"))));

    storageConfig.put(STORAGE_CONFIGS_KEY_HIVE_SITE, new ConfigFileItem(
        ConfigFileProperties.CATALOG_HIVE_SITE + ".xml",
        constructCatalogConfigFileUrl(catalogName, CONFIG_TYPE_STORAGE,
            STORAGE_CONFIGS_KEY_HIVE_SITE.replace("\\.", "-"))));

    storageConfig.put(STORAGE_CONFIGS_KEY_TYPE, config.get(STORAGE_CONFIGS_KEY_TYPE));
    return storageConfig;
  }

  /**
   * Construct catalog meta through catalog register info.
   */
  private static CatalogMeta constructCatalogMeta(CatalogRegisterInfo info, CatalogMeta oldCatalogMeta) {
    CatalogMeta catalogMeta = new CatalogMeta();
    catalogMeta.setCatalogName(info.getName());
    catalogMeta.setCatalogType(info.getType());
    catalogMeta.setCatalogProperties(info.getProperties());
    if (info.getTableFormatList() == null || info.getTableFormatList().isEmpty()) {
      throw new RuntimeException("Invalid table format list");
    }
    StringBuilder tableFormats = new StringBuilder();
    try {
      // validate table format
      info.getTableFormatList().forEach(item -> tableFormats.append(TableFormat.valueOf(item).name()));
    } catch (Exception e) {
      throw new RuntimeException("Invalid table format list, " + String.join(",", info.getTableFormatList()));
    }
    List<String> requiredProperties = CATALOG_REQUIRED_PROPERTIES.get(info.getType());
    if (requiredProperties != null && !requiredProperties.isEmpty()) {
      for (String propertyName : requiredProperties) {
        if (!info.getProperties().containsKey(propertyName)) {
          throw new RuntimeException(
              String.format("Catalog type:%s require property:%s.", info.getType(), propertyName));
        }
      }
    }
    catalogMeta.getCatalogProperties().put(CatalogMetaProperties.TABLE_FORMATS, tableFormats.toString());
    catalogMeta.setAuthConfigs(authConvertFromServerToMeta(info.getAuthConfig(), oldCatalogMeta));
    // change fileId to base64Code
    Map<String, String> metaStorageConfig = new HashMap<>();
    metaStorageConfig.put(STORAGE_CONFIGS_KEY_TYPE,
            info.getStorageConfig().getOrDefault(STORAGE_CONFIGS_KEY_TYPE, STORAGE_CONFIGS_VALUE_TYPE_HDFS));

    List<String> metaKeyList = Arrays.asList(
        STORAGE_CONFIGS_KEY_HDFS_SITE,
        STORAGE_CONFIGS_KEY_CORE_SITE,
        STORAGE_CONFIGS_KEY_HIVE_SITE);

    // when update catalog, fileId won't be post when file doesn't been changed!
    int idx;
    boolean fillUseOld = oldCatalogMeta != null;
    for (idx = 0; idx < metaKeyList.size(); idx++) {
      String fileId = info.getStorageConfig()
          .get(metaKeyList.get(idx));
      if (!StringUtils.isEmpty(fileId)) {
        String fileSite = platformFileInfoService.getFileContentB64ById(Integer.valueOf(fileId));
        metaStorageConfig.put(metaKeyList.get(idx), fileSite);
      } else {
        if (fillUseOld) {
          String fileSite = oldCatalogMeta.getStorageConfigs().get(metaKeyList.get(idx));
          metaStorageConfig.put(metaKeyList.get(idx), fileSite);
        }
      }
    }
    // if site.xml is not present, we give a default value!
    if (StringUtils.isEmpty(metaStorageConfig.get(STORAGE_CONFIGS_KEY_HIVE_SITE))) {
      metaStorageConfig.put(STORAGE_CONFIGS_KEY_HIVE_SITE, EMPTY_XML_BASE64);
    }

    if (StringUtils.isEmpty(metaStorageConfig.get(STORAGE_CONFIGS_KEY_HDFS_SITE))) {
      metaStorageConfig.put(STORAGE_CONFIGS_KEY_HDFS_SITE, EMPTY_XML_BASE64);
    }

    if (StringUtils.isEmpty(metaStorageConfig.get(STORAGE_CONFIGS_KEY_CORE_SITE))) {
      metaStorageConfig.put(STORAGE_CONFIGS_KEY_CORE_SITE, EMPTY_XML_BASE64);
    }
    catalogMeta.setStorageConfigs(metaStorageConfig);
    return catalogMeta;
  }

  /**
   * Register catalog to ams.
   */
  public static void createCatalog(Context ctx) {
    CatalogRegisterInfo info = ctx.bodyAsClass(CatalogRegisterInfo.class);
    if (info.getAuthConfig() == null ||
        info.getStorageConfig() == null ||
        info.getProperties() == null) {
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Some configuration is null", null));
    }
    if (catalogMetadataService.catalogExist(info.getName())) {
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Duplicate catalog name!", null));
      return;
    }
    try {
      CatalogMeta catalogMeta = constructCatalogMeta(info, null);
      catalogMetadataService.addCatalog(catalogMeta);
      ctx.json(OkResponse.of(""));
    } catch (Exception e) {
      LOG.error("Failed to create catalog!", e);
      ctx.json(new ErrorResponse(e.getMessage()));
    }
  }

  /**
   * Get detail of some catalog.
   */
  public static void getCatalogDetail(Context ctx) {
    String catalogName = ctx.pathParam("catalogName");
    Optional<CatalogMeta> optCatalog = catalogMetadataService.getCatalog(catalogName);
    if (!optCatalog.isPresent()) {
      ctx.json(OkResponse.of(null));
      return;
    }
    CatalogMeta catalogMeta = optCatalog.get();
    CatalogSettingInfo info = new CatalogSettingInfo();

    if (catalogMetadataService.catalogExist(catalogName)) {
      info.setName(catalogMeta.getCatalogName());
      // We create ams catalog with type hadoop in v0.3, we should be compatible with it.
      if (CATALOG_TYPE_HADOOP.equals(catalogMeta.getCatalogType()) &&
          !catalogMeta.getCatalogProperties().containsKey(TABLE_FORMATS)) {
        info.setType(CATALOG_TYPE_AMS);
      } else {
        info.setType(catalogMeta.getCatalogType());
      }
      info.setAuthConfig(authConvertFromMetaToServer(catalogName, catalogMeta.getAuthConfigs()));
      Map<String, Object> storageConfig = storageConvertFromMetaToServer(catalogName, catalogMeta.getStorageConfigs());
      info.setStorageConfig(storageConfig);
      // we put the table format single
      String tableFormat = catalogMeta.getCatalogProperties().get(CatalogMetaProperties.TABLE_FORMATS);
      if (StringUtils.isEmpty(tableFormat)) {
        if (catalogMeta.getCatalogType().equals(CATALOG_TYPE_HIVE)) {
          tableFormat = TableFormat.MIXED_HIVE.name();
        } else {
          tableFormat = TableFormat.MIXED_ICEBERG.name();
        }
      }
      info.setTableFormatList(Arrays.asList(tableFormat.split(",")));
      info.setProperties(catalogMeta.getCatalogProperties());
      info.getProperties().remove(CatalogMetaProperties.TABLE_FORMATS);
      ctx.json(OkResponse.of(info));
      return;
    }
    ctx.json(OkResponse.of(null));
  }

  /**
   * Get detail of some catalog
   * 1、first check whether there are some tables in catalog.
   */
  public static void updateCatalog(Context ctx) {
    CatalogRegisterInfo info = ctx.bodyAsClass(CatalogRegisterInfo.class);
    if (info.getAuthConfig() == null ||
        info.getStorageConfig() == null) {
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Some configuration is null", null));
    }
    Optional<CatalogMeta> optCatalog = catalogMetadataService.getCatalog(info.getName());
    if (!optCatalog.isPresent()) {
      ctx.json(new ErrorResponse(HttpCode.BAD_REQUEST, "Catalog doesn't exist!", null));
      return;
    }
    CatalogMeta oldCatalogMeta = optCatalog.get();

    // check only some item can be modified!
    try {
      CatalogMeta catalogMeta = constructCatalogMeta(info, oldCatalogMeta);
      catalogMetadataService.updateCatalog(catalogMeta);
      CatalogUtil.removeCatalogCache(catalogMeta.getCatalogName());
    } catch (Exception e) {
      LOG.error("Failed to update catalog!", e);
      ctx.json(new ErrorResponse(e.getMessage()));
    }
    ctx.json(OkResponse.of(null));
  }

  /**
   * Check whether we could delete the catalog
   */
  public static void catalogDeleteCheck(Context ctx) {
    // check
    Integer tblCount;
    if (StringUtils.isNotEmpty(ctx.pathParam("catalogName"))) {
      tblCount = iMetaService.getTableCountInCatalog(ctx.pathParam("catalogName"));
    } else {
      ctx.json(new ErrorResponse(HttpCode.FORBIDDEN, "Catalog name is empty!", null));
      return;
    }
    ctx.json(OkResponse.of(tblCount <= 0));
  }

  /**
   * Delete some catalog and information associate with the catalog
   */
  public static void deleteCatalog(Context ctx) {
    String catalogName = ctx.pathParam("catalogName");
    // need check
    if (StringUtils.isEmpty(catalogName)) {
      ctx.json(new ErrorResponse(HttpCode.FORBIDDEN, "Catalog name is empty!", null));
      return;
    }
    Integer tblCount = iMetaService.getTableCountInCatalog(catalogName);
    if (tblCount == 0) {
      catalogMetadataService.deleteCatalog(catalogName);
      CatalogUtil.removeCatalogCache(catalogName);
      ctx.json(OkResponse.of("OK"));
    } else {
      ctx.json(new ErrorResponse(HttpCode.FORBIDDEN, "Some tables in catalog!", null));
    }
  }

  /**
   * Construct a url
   */
  public static String constructCatalogConfigFileUrl(String catalogName, String type, String key) {
    return String.format("/ams/v1/catalogs/%s/config/%s/%s", catalogName,
        type, key.replaceAll("\\.", "-"));
  }

  /**
   * get file content of authconfig/storageconfig config file
   * get("/catalogs/{catalogName}/config/{type}/{key}
   */
  public static void getCatalogConfFileContent(Context ctx) {
    String catalogName = ctx.pathParam("catalogName");
    String confType = ctx.pathParam("type");
    String configKey = ctx.pathParam("key");

    if (StringUtils.isEmpty(catalogName) || StringUtils.isEmpty(confType) || StringUtils.isEmpty(configKey)) {
      ctx.json(new ErrorResponse("Catalog name or auth type or config key is null!"));
      return;
    }

    // get file content from catlaog.
    Optional<CatalogMeta> optCatalog = catalogMetadataService.getCatalog(catalogName);
    if (!optCatalog.isPresent()) {
      throw new IllegalArgumentException("catalog is not exist");
    }
    CatalogMeta catalogMeta = optCatalog.get();
    if (CONFIG_TYPE_STORAGE.equalsIgnoreCase(confType)) {
      Map<String, String> storageConfig = catalogMeta.getStorageConfigs();
      String key = configKey.replaceAll("-", "\\.");
      ctx.result(new String(Base64.getDecoder().decode(storageConfig.get(key))));
    } else if (CONFIG_TYPE_AUTH.equalsIgnoreCase(confType)) {
      Map<String, String> storageConfig = catalogMeta.getAuthConfigs();
      String key = configKey.replaceAll("-", "\\.");
      ctx.result(new String(Base64.getDecoder().decode(storageConfig.get(key))));
    } else {
      ctx.json(new ErrorResponse("Invalid request for " + confType));
    }
  }
}
