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

package com.netease.arctic.ams.server.model;

import com.google.common.annotations.VisibleForTesting;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.TableMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.ams.api.properties.MetaTableProperties;
import com.netease.arctic.ams.server.utils.PropertiesUtil;
import com.netease.arctic.table.PrimaryKeySpec;
import com.netease.arctic.table.TableIdentifier;
import com.netease.arctic.table.TableMetaStore;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static com.netease.arctic.table.PrimaryKeySpec.PRIMARY_KEY_COLUMN_JOIN_DELIMITER;

public class TableMetadata implements Serializable {
  private static final Logger LOG = LoggerFactory.getLogger(TableMetadata.class);

  public TableMetadata() {
  }

  public TableMetadata(
      TableIdentifier tableIdentifier, TableMetaStore metaStore, String tableLocation,
      String baseLocation, String changeLocation, String primaryKey,
      String metaStoreSite, String hdfsSite, String coreSite,
      String authMethod, String hadoopUsername, String krbKeyteb, String krbConf,
      String krbPrincipal, Map<String, String> properties) {
    this.tableIdentifier = tableIdentifier;
    this.metaStore = metaStore;
    this.tableLocation = tableLocation;
    this.baseLocation = baseLocation;
    this.changeLocation = changeLocation;
    this.primaryKey = primaryKey;
    this.metaStoreSite = metaStoreSite;
    this.hdfsSite = hdfsSite;
    this.coreSite = coreSite;
    this.authMethod = authMethod;
    this.hadoopUsername = hadoopUsername;
    this.krbKeyteb = krbKeyteb;
    this.krbConf = krbConf;
    this.krbPrincipal = krbPrincipal;
    this.properties = ImmutableMap.copyOf(properties);
  }

  public TableMetadata(TableMeta tableMeta, CatalogMeta catalogMeta) {
    this.tableIdentifier = new TableIdentifier(tableMeta.getTableIdentifier());
    if (tableMeta.getLocations() != null &&
        tableMeta.getLocations().containsKey(MetaTableProperties.LOCATION_KEY_TABLE)) {
      this.tableLocation = tableMeta.getLocations().get(MetaTableProperties.LOCATION_KEY_TABLE);
    }
    if (tableMeta.getLocations() != null &&
        tableMeta.getLocations().containsKey(MetaTableProperties.LOCATION_KEY_BASE)) {
      this.baseLocation = tableMeta.getLocations().get(MetaTableProperties.LOCATION_KEY_BASE);
    }
    if (tableMeta.getLocations() != null &&
        tableMeta.getLocations().containsKey(MetaTableProperties.LOCATION_KEY_CHANGE)) {
      this.changeLocation = tableMeta.getLocations().get(MetaTableProperties.LOCATION_KEY_CHANGE);
    }
    if (StringUtils.isBlank(this.tableLocation) || StringUtils.isBlank(this.baseLocation)) {
      throw new IllegalArgumentException("table location is required");
    }

    if (tableMeta.getKeySpec() == null || CollectionUtils.isEmpty(tableMeta.getKeySpec().getFields())) {
      this.primaryKey = PrimaryKeySpec.noPrimaryKey().description();
    } else {
      this.primaryKey = String.join(PRIMARY_KEY_COLUMN_JOIN_DELIMITER, tableMeta.getKeySpec().getFields());
    }
    this.metaStoreSite = catalogMeta.getStorageConfigs().get(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE);
    this.hdfsSite = catalogMeta.getStorageConfigs().get(CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE);
    this.coreSite = catalogMeta.getStorageConfigs().get(CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE);
    this.authMethod = catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE);
    if (this.authMethod != null) {
      this.authMethod = this.authMethod.toUpperCase(Locale.ROOT);
    }
    this.hadoopUsername = catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME);
    this.krbKeyteb = catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB);
    this.krbConf = catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB5);
    this.krbPrincipal = catalogMeta.getAuthConfigs().get(CatalogMetaProperties.AUTH_CONFIGS_KEY_PRINCIPAL);
    this.properties = tableMeta.getProperties();
  }

  public TableMeta buildTableMeta() {
    TableMeta meta = new TableMeta();
    meta.setTableIdentifier(tableIdentifier.buildTableIdentifier());
    Map<String, String> locations = new HashMap<>();
    PropertiesUtil.putNotNullProperties(locations, MetaTableProperties.LOCATION_KEY_TABLE, tableLocation);
    PropertiesUtil.putNotNullProperties(locations, MetaTableProperties.LOCATION_KEY_CHANGE, changeLocation);
    PropertiesUtil.putNotNullProperties(locations, MetaTableProperties.LOCATION_KEY_BASE, baseLocation);
    meta.setLocations(locations);

    Map<String, String> newProperties = new HashMap<>(properties);
    meta.setProperties(newProperties);

    if (StringUtils.isNotBlank(primaryKey)) {
      com.netease.arctic.ams.api.PrimaryKeySpec keySpec =
          new com.netease.arctic.ams.api.PrimaryKeySpec();
      List<String> fields = Arrays.stream(primaryKey.split(PRIMARY_KEY_COLUMN_JOIN_DELIMITER))
          .collect(Collectors.toList());
      keySpec.setFields(fields);
      meta.setKeySpec(keySpec);
    }
    return meta;
  }

  private TableIdentifier tableIdentifier;

  private String tableLocation;

  private String baseLocation;

  private String changeLocation;

  private String primaryKey;

  private String metaStoreSite;

  private String hdfsSite;

  private String coreSite;

  private String authMethod;

  private String hadoopUsername;

  private String krbKeyteb;

  private String krbConf;

  private String krbPrincipal;

  private Map<String, String> properties;

  private long currentTxId;

  private volatile TableMetaStore metaStore;

  public String getTableLocation() {
    return tableLocation;
  }

  public void setTableLocation(String tableLocation) {
    this.tableLocation = tableLocation;
  }

  public String getBaseLocation() {
    return baseLocation;
  }

  public void setBaseLocation(String baseLocation) {
    this.baseLocation = baseLocation;
  }

  public String getChangeLocation() {
    return changeLocation;
  }

  public void setChangeLocation(String changeLocation) {
    this.changeLocation = changeLocation;
  }

  public String getPrimaryKey() {
    return primaryKey;
  }

  public void setPrimaryKey(String primaryKey) {
    this.primaryKey = primaryKey;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public TableIdentifier getTableIdentifier() {
    return tableIdentifier;
  }

  public void setTableIdentifier(TableIdentifier tableIdentifier) {
    this.tableIdentifier = tableIdentifier;
  }

  public TableMetaStore getMetaStore() {
    if (metaStore == null) {
      synchronized (this) {
        if (metaStore == null) {
          this.metaStore = TableMetaStore.builder()
              .withBase64MetaStoreSite(metaStoreSite)
              .withBase64CoreSite(coreSite)
              .withBase64HdfsSite(hdfsSite)
              .withBase64Auth(authMethod, hadoopUsername, krbKeyteb, krbConf, krbPrincipal)
              .build();
        }
      }
    }

    return metaStore;
  }

  @VisibleForTesting
  public void setMetaStore(TableMetaStore metaStore) {
    synchronized (this) {
      this.metaStore = metaStore;
    }
  }

  public String getMetaStoreSite() {
    return metaStoreSite;
  }

  public void setMetaStoreSite(String metaStoreSite) {
    this.metaStoreSite = metaStoreSite;
  }

  public String getHdfsSite() {
    return hdfsSite;
  }

  public void setHdfsSite(String hdfsSite) {
    this.hdfsSite = hdfsSite;
  }

  public String getCoreSite() {
    return coreSite;
  }

  public void setCoreSite(String coreSite) {
    this.coreSite = coreSite;
  }

  public String getAuthMethod() {
    return authMethod;
  }

  public void setAuthMethod(String authMethod) {
    this.authMethod = authMethod;
  }

  public String getHadoopUsername() {
    return hadoopUsername;
  }

  public void setHadoopUsername(String hadoopUsername) {
    this.hadoopUsername = hadoopUsername;
  }

  public String getKrbKeyteb() {
    return krbKeyteb;
  }

  public void setKrbKeyteb(String krbKeyteb) {
    this.krbKeyteb = krbKeyteb;
  }

  public String getKrbConf() {
    return krbConf;
  }

  public void setKrbConf(String krbConf) {
    this.krbConf = krbConf;
  }

  public String getKrbPrincipal() {
    return krbPrincipal;
  }

  public void setKrbPrincipal(String krbPrincipal) {
    this.krbPrincipal = krbPrincipal;
  }

  public long getCurrentTxId() {
    return currentTxId;
  }

  public void setCurrentTxId(long currentTxId) {
    this.currentTxId = currentTxId;
  }
}
