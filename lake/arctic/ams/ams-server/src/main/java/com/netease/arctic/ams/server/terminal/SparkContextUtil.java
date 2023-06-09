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

package com.netease.arctic.ams.server.terminal;

import com.netease.arctic.ams.server.config.Configuration;
import com.netease.arctic.spark.ArcticSparkCatalog;
import com.netease.arctic.spark.ArcticSparkExtensions;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;
import org.apache.iceberg.spark.SparkCatalog;
import org.apache.iceberg.spark.extensions.IcebergSparkSessionExtensions;

import java.util.List;
import java.util.Map;

public class SparkContextUtil {

  public static Map<String, String> getSparkConf(Configuration sessionConfig) {
    Map<String, String> sparkConf = Maps.newLinkedHashMap();
    sparkConf.put("spark.sql.extensions", ArcticSparkExtensions.class.getName() +
        "," + IcebergSparkSessionExtensions.class.getName());

    List<String> catalogs = sessionConfig.get(TerminalSessionFactory.SessionConfigOptions.CATALOGS);
    String catalogUrlBase = sessionConfig.get(TerminalSessionFactory.SessionConfigOptions.CATALOG_URL_BASE);

    for (String catalog : catalogs) {
      String connector = sessionConfig.get(TerminalSessionFactory.SessionConfigOptions.catalogConnector(catalog));
      if ("iceberg".equalsIgnoreCase(connector)) {
        sparkConf.put("spark.sql.catalog." + catalog, SparkCatalog.class.getName());

        Map<String, String> properties =
            TerminalSessionFactory.SessionConfigOptions.getCatalogProperties(sessionConfig, catalog);
        for (String key : properties.keySet()) {
          String property = properties.get(key);
          sparkConf.put("spark.sql.catalog." + catalog + "." + key, property);
        }
      } else {
        sparkConf.put("spark.sql.catalog." + catalog, ArcticSparkCatalog.class.getName());
        sparkConf.put("spark.sql.catalog." + catalog + ".url", catalogUrlBase + catalog);
      }
    }
    return sparkConf;
  }
}
