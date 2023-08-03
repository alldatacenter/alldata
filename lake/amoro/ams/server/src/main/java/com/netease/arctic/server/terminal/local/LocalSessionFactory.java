/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *  *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.server.terminal.local;

import com.netease.arctic.server.terminal.SparkContextUtil;
import com.netease.arctic.server.terminal.TerminalSession;
import com.netease.arctic.server.terminal.TerminalSessionFactory;
import com.netease.arctic.server.utils.ConfigOptions;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.spark.ArcticSparkExtensions;
import com.netease.arctic.table.TableMetaStore;
import org.apache.iceberg.relocated.com.google.common.base.Preconditions;
import org.apache.iceberg.relocated.com.google.common.collect.Lists;
import org.apache.iceberg.relocated.com.google.common.collect.Sets;
import org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions;
import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.internal.SQLConf;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LocalSessionFactory implements TerminalSessionFactory {

  static final Set<String> STATIC_SPARK_CONF = Collections.unmodifiableSet(
      Sets.newHashSet("spark.sql.extensions")
  );
  static final String SPARK_CONF_PREFIX = "spark.";

  SparkSession context = null;
  Configurations conf;

  @Override
  public void initialize(Configurations properties) {
    this.conf = properties;
  }

  @Override
  public TerminalSession create(TableMetaStore metaStore, Configurations configuration) {
    SparkSession context = lazyInitContext();
    SparkSession session = context.cloneSession();
    List<String> catalogs = configuration.get(SessionConfigOptions.CATALOGS);
    List<String> initializeLogs = Lists.newArrayList();
    initializeLogs.add("setup session, session factory: " + LocalSessionFactory.class.getName());

    Map<String, String> sparkConf = SparkContextUtil.getSparkConf(configuration);
    sparkConf.put(com.netease.arctic.spark.SparkSQLProperties.REFRESH_CATALOG_BEFORE_USAGE, "true");

    Map<String, String> finallyConf = configuration.toMap();
    catalogs.stream()
        .filter(c -> isIcebergCatalog(c, configuration))
        .forEach(c -> setHadoopConfigToSparkSession(c, session, metaStore));

    for (String key : sparkConf.keySet()) {
      if (STATIC_SPARK_CONF.contains(key)) {
        continue;
      }
      updateSessionConf(session, initializeLogs, key, sparkConf.get(key));
      finallyConf.put(key, sparkConf.get(key));
    }

    return new LocalTerminalSession(catalogs, session, initializeLogs, finallyConf);
  }

  private boolean isIcebergCatalog(String catalog, Configurations configurations) {
    String connector = configurations.get(TerminalSessionFactory.SessionConfigOptions.catalogConnector(catalog));
    return "iceberg".equalsIgnoreCase(connector);
  }

  private void setHadoopConfigToSparkSession(String catalog, SparkSession session, TableMetaStore metaStore) {
    org.apache.hadoop.conf.Configuration metaConf = metaStore.getConfiguration();
    for (Map.Entry<String, String> next : metaConf) {
      session.conf().set("spark.sql.catalog." + catalog + ".hadoop." + next.getKey(), next.getValue());
    }
  }

  private void updateSessionConf(SparkSession session, List<String> logs, String key, String value) {
    session.conf().set(key, value);
    logs.add(key + "  " + value);
  }

  protected synchronized SparkSession lazyInitContext() {
    Preconditions.checkNotNull(this.conf);
    if (context == null) {
      SparkConf sparkconf = new SparkConf()
          .setAppName("spark-local-context")
          .setMaster("local");
      sparkconf.set(SQLConf.PARTITION_OVERWRITE_MODE().key(), "dynamic");
      sparkconf.set("spark.executor.heartbeatInterval", "100s");
      sparkconf.set("spark.network.timeout", "200s");
      sparkconf.set("spark.sql.extensions", ArcticSparkExtensions.class.getName() +
          "," + IcebergSparkSessionExtensions.class.getName());

      for (String key : this.conf.keySet()) {
        if (key.startsWith(SPARK_CONF_PREFIX)) {
          String value = this.conf.getValue(ConfigOptions.key(key).stringType().noDefaultValue());
          sparkconf.set(key, value);
        }
      }

      context = SparkSession
          .builder()
          .config(sparkconf)
          .getOrCreate();
      context.sparkContext().setLogLevel("WARN");
    }

    return context;
  }
}
