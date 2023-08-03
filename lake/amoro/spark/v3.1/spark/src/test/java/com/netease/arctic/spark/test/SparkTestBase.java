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

import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import com.netease.arctic.spark.ArcticSparkSessionCatalog;
import org.apache.iceberg.relocated.com.google.common.collect.ImmutableMap;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.execution.QueryExecution;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;


public class SparkTestBase {
  protected static final Logger LOG = LoggerFactory.getLogger(SparkTestBase.class);
  public static final SparkTestContext context = new SparkTestContext();
  public static final String SESSION_CATALOG = "spark_catalog";
  public static final String INTERNAL_CATALOG = "arctic_catalog";
  public static final String HIVE_CATALOG = "hive_catalog";

  @BeforeAll
  public static void setupContext() throws Exception {
    context.initialize();
  }

  @AfterAll
  public static void tearDownContext() {
    context.close();
  }

  private SparkSession spark;
  private ArcticCatalog catalog;
  protected String currentCatalog = SESSION_CATALOG;
  protected QueryExecution qe;


  protected Map<String, String> sparkSessionConfig() {
    return ImmutableMap.of(
        "spark.sql.catalog.spark_catalog", ArcticSparkSessionCatalog.class.getName(),
        "spark.sql.catalog.spark_catalog.url", context.catalogUrl(SparkTestContext.EXTERNAL_HIVE_CATALOG_NAME)
    );
  }


  @AfterEach
  public void tearDownTestSession() {
    spark = null;
    catalog = null;
  }

  public void setCurrentCatalog(String catalog) {
    this.currentCatalog = catalog;
    sql("USE " + this.currentCatalog);
    this.catalog = null;
  }

  protected ArcticCatalog catalog() {
    if (catalog == null) {
      String catalogUrl = spark().sessionState().conf().getConfString(
          "spark.sql.catalog." + currentCatalog + ".url");
      catalog = CatalogLoader.load(catalogUrl);
    }
    return catalog;
  }

  protected SparkSession spark() {
    if (this.spark == null) {
      Map<String, String> conf = sparkSessionConfig();
      this.spark = context.getSparkSession(conf);
    }
    return spark;
  }


  public Dataset<Row> sql(String sqlText) {
    long begin = System.currentTimeMillis();
    LOG.info("Execute SQL: " + sqlText);
    Dataset<Row> ds = spark().sql(sqlText);
    if (ds.columns().length == 0) {
      LOG.info("+----------------+");
      LOG.info("|  Empty Result  |");
      LOG.info("+----------------+");
    } else {
      ds.show();
    }
    qe = ds.queryExecution();
    LOG.info("SQL Execution cost: " + (System.currentTimeMillis() - begin) + " ms");
    return ds;
  }
}
