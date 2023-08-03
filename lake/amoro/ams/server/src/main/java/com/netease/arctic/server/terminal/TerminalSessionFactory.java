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

package com.netease.arctic.server.terminal;

import com.google.common.collect.Maps;
import com.netease.arctic.server.utils.ConfigOption;
import com.netease.arctic.server.utils.ConfigOptions;
import com.netease.arctic.server.utils.Configurations;
import com.netease.arctic.table.TableMetaStore;

import java.util.List;
import java.util.Map;

/**
 * factory to create a TerminalSession
 */
public interface TerminalSessionFactory {

  /**
   * this will be called after factory is created.
   *
   * @param properties - terminal properties and factory properties.
   */
  void initialize(Configurations properties);

  /**
   * create a new session
   *
   * @param metaStore     - auth info
   * @param configuration - configuration of session, all properties are defined in {@link SessionConfigOptions}
   * @return - new terminal context
   */
  TerminalSession create(TableMetaStore metaStore, Configurations configuration);

  class SessionConfigOptions {
    public static ConfigOption<Integer> FETCH_SIZE = ConfigOptions
        .key("session.fetch-size")
        .intType()
        .defaultValue(1000);

    public static ConfigOption<List<String>> CATALOGS = ConfigOptions
        .key("session.catalogs")
        .stringType()
        .asList()
        .noDefaultValue();

    public static ConfigOption<String> CATALOG_URL_BASE = ConfigOptions
        .key("catalog-url-base")
        .stringType()
        .noDefaultValue();

    public static ConfigOption<String> catalogConnector(String catalog) {
      return ConfigOptions.key("session.catalog." + catalog + ".connector")
          .stringType()
          .noDefaultValue();
    }

    public static ConfigOption<String> catalogType(String catalog) {
      return ConfigOptions.key("session.catalog." + catalog + ".type")
          .stringType()
          .noDefaultValue();
    }

    public static ConfigOption<Boolean> USING_SESSION_CATALOG_FOR_HIVE = ConfigOptions
        .key("using-session-catalog-for-hive")
        .booleanType()
        .defaultValue(false);

    public static ConfigOption<String> catalogProperty(String catalog, String propertyKey) {
      return ConfigOptions.key("catalog." + catalog + "." + propertyKey)
          .stringType()
          .noDefaultValue();
    }

    public static Map<String, String> getCatalogProperties(Configurations configuration, String catalog) {
      final String prefix = "catalog." + catalog + ".";
      Map<String, String> properties = Maps.newHashMap();
      for (String key : configuration.keySet()) {
        if (key.startsWith(prefix)) {
          ConfigOption<String> confOption = ConfigOptions.key(key)
              .stringType()
              .noDefaultValue();
          properties.put(key.substring(prefix.length()), configuration.getString(confOption));
        }
      }
      return properties;
    }
  }

  ConfigOption<Integer> FETCH_SIZE = ConfigOptions.key("fetch-size")
      .intType()
      .defaultValue(1000);
}
