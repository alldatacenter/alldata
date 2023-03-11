package com.netease.arctic.spark.hive;

import com.netease.arctic.ams.api.CatalogMeta;
import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hive.conf.HiveConf;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
public class HiveCatalogMetaTestUtil {
  public static CatalogMeta createArcticCatalog(File arcticBaseDir ,HiveConf entries) throws IOException {
    CatalogMeta meta = new CatalogMeta();
    meta.setCatalogName("arctic_hive");
    meta.setCatalogType(CatalogMetaProperties.CATALOG_TYPE_HIVE);
    Map<String, String> storageConfig = new HashMap<>();
    storageConfig.put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.STORAGE_CONFIGS_VALUE_TYPE_HDFS);

    storageConfig.put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_CORE_SITE,
        encodingSite(new Configuration()));
    storageConfig.put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_HDFS_SITE,
        encodingSite(new Configuration()));
    storageConfig.put(
        CatalogMetaProperties.STORAGE_CONFIGS_KEY_HIVE_SITE,
        encodingSite(entries));
    meta.setStorageConfigs(storageConfig);

    meta.putToAuthConfigs(
        CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE,
        CatalogMetaProperties.AUTH_CONFIGS_VALUE_TYPE_SIMPLE);
    meta.putToAuthConfigs(
        CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME,
        System.getProperty("user.name"));

    meta.putToCatalogProperties(
        CatalogMetaProperties.KEY_WAREHOUSE,
        arcticBaseDir.getAbsolutePath()
    );
    return meta;
  }

  public static String encodingSite(Configuration conf) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    conf.writeXml(out);
    String hiveSite = out.toString();
    return Base64.getEncoder().encodeToString(hiveSite.getBytes(StandardCharsets.UTF_8));
  }
}
