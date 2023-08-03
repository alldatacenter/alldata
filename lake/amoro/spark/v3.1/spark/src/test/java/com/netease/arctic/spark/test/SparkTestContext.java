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

package com.netease.arctic.spark.test;


import com.netease.arctic.CatalogMetaTestUtil;
import com.netease.arctic.SingletonResourceUtil;
import com.netease.arctic.TestAms;
import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.hive.TestHMS;
import com.netease.arctic.spark.ArcticSparkCatalog;
import com.netease.arctic.spark.ArcticSparkExtensions;
import com.netease.arctic.spark.test.helper.HiveCatalogMetaTestUtil;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.NoSuchObjectException;
import org.apache.hadoop.hive.metastore.api.Table;
import org.apache.iceberg.exceptions.NoSuchTableException;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.apache.thrift.TException;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.util.Map;

public class SparkTestContext {

  final TemporaryFolder warehouse = new TemporaryFolder();
  public static final String INTERNAL_CATALOG_NAME = "arctic_catalog";
  public static final String EXTERNAL_HIVE_CATALOG_NAME = "hive_catalog";

  final TestAms ams = new TestAms();

  final TestHMS hms = new TestHMS();

  private boolean catalogSet = false;
  private SparkSession spark;
  private Map<String, String> sparkConf;

  public void initialize() throws Exception {
    ams.before();
    hms.before();
    warehouse.create();
    setupCatalogs();
  }

  public void close() {
    if (!SingletonResourceUtil.isUseSingletonResource()) {
      ams.getAmsHandler().cleanUp();
      catalogSet = false;
    }
    ams.after();
    hms.after();
  }

  public Table loadHiveTable(String database, String table) {
    try {
      return hms.getHiveClient().getTable(database, table);
    } catch (NoSuchObjectException e) {
      throw new NoSuchTableException(e, database + "." + table);
    } catch (TException e) {
      throw new RuntimeException(e);
    }
  }

  public void dropHiveTable(String database, String table) {
    try {
      hms.getHiveClient().dropTable(database, table, true, true);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public HiveMetaStoreClient getHiveClient() {
    return hms.getHiveClient();
  }

  private void setupCatalogs() throws IOException {
    if (SingletonResourceUtil.isUseSingletonResource()) {
      if (catalogSet) {
        return;
      }
    }
    CatalogMeta arcticCatalogMeta = CatalogMetaTestUtil.createArcticCatalog(warehouse.getRoot());
    arcticCatalogMeta.setCatalogName(amsCatalogName("ARCTIC"));
    ams.getAmsHandler().createCatalog(arcticCatalogMeta);

    HiveConf hiveConf = hms.getHiveConf();
    CatalogMeta hiveCatalogMeta = HiveCatalogMetaTestUtil.createArcticCatalog(warehouse.getRoot(), hiveConf);
    hiveCatalogMeta.setCatalogName(amsCatalogName("HIVE"));
    ams.getAmsHandler().createCatalog(hiveCatalogMeta);
    catalogSet = true;
  }


  public String amsCatalogName(String catalogType) {
    if ("Hive".equalsIgnoreCase(catalogType)) {
      return EXTERNAL_HIVE_CATALOG_NAME;
    } else if ("Arctic".equalsIgnoreCase(catalogType)) {
      return INTERNAL_CATALOG_NAME;
    } else {
      throw new IllegalArgumentException("unknown type of catalog type:" + catalogType);
    }
  }


  public String catalogUrl(String arcticCatalogName) {
    return this.ams.getServerUrl() + "/" + arcticCatalogName;
  }

  private String hiveVersion() {
    try {
      return SparkTestContext.class.getClassLoader()
          .loadClass("org.apache.hadoop.hive.metastore.HiveMetaStoreClient")
          .getPackage()
          .getImplementationVersion();
    } catch (ClassNotFoundException e) {
      throw new RuntimeException(e);
    }
  }

  private String hiveMetastoreUri() {
    return "thrift://127.0.0.1:" + hms.getMetastorePort();
  }

  private String warehouseDir() {
    return this.warehouse.getRoot().getAbsolutePath();
  }

  public SparkSession getSparkSession(Map<String, String> externalConfigs) {
    Map<String, String> configs = Maps.newHashMap();
    configs.put("spark.sql.catalog." + amsCatalogName("arctic"), ArcticSparkCatalog.class.getName());
    configs.put("spark.sql.catalog." + amsCatalogName("arctic") + ".url",
        this.ams.getServerUrl() + "/" + amsCatalogName("arctic"));
    configs.put("spark.sql.catalog." + amsCatalogName("hive"), ArcticSparkCatalog.class.getName());
    configs.put("spark.sql.catalog." + amsCatalogName("hive") + ".url",
        this.ams.getServerUrl() + "/" + amsCatalogName("hive"));

    configs.put("hive.metastore.uris", this.hiveMetastoreUri());
    configs.put("spark.sql.catalogImplementation", "hive");
    configs.put("spark.sql.hive.metastore.version", this.hiveVersion());
    configs.put("spark.sql.hive.metastore.jars", "maven");
    configs.put("hive.metastore.client.capability.check", "false");

    configs.put("spark.executor.heartbeatInterval", "500s");
    configs.put("spark.cores.max", "6");
    configs.put("spark.executor.cores", "2");
    configs.put("spark.default.parallelism", "12");
    configs.put("spark.network.timeout", "600s");
    configs.put("spark.sql.warehouse.dir", this.warehouseDir());
    configs.put("spark.sql.extensions", ArcticSparkExtensions.class.getName());
    configs.put("spark.testing.memory", "943718400");

    if (externalConfigs != null) {
      configs.putAll(externalConfigs);
    }

    initializeSparkSession(configs);
    return this.spark.cloneSession();
  }

  private boolean isSameSparkConf(Map<String, String> sparkConf) {
    if (this.sparkConf == null) {
      return false;
    }
    if (this.sparkConf.size() != sparkConf.size()) {
      return false;
    }
    for (String key : sparkConf.keySet()) {
      String newValue = sparkConf.get(key);
      String existedValue = this.sparkConf.get(key);
      if (!StringUtils.equals(newValue, existedValue)) {
        return false;
      }
    }
    return true;
  }

  private void cleanLocalSparkContext() {
    if (spark != null) {
      spark.close();
      spark = null;
    }
  }

  private void initializeSparkSession(Map<String, String> sparkConf) {
    boolean create = spark == null;
    if (!isSameSparkConf(sparkConf)) {
      create = true;
    }
    if (create) {
      cleanLocalSparkContext();

      SparkConf sparkconf = new SparkConf()
          .setAppName("arctic-spark-unit-tests")
          .setMaster("local[*]");
      sparkConf.forEach(sparkconf::set);
      spark = SparkSession
          .builder()
          .config(sparkconf)
          .getOrCreate();
      spark.sparkContext().setLogLevel("WARN");
      this.sparkConf = sparkConf;
    }
  }
}
