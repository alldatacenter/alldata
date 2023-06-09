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

package com.netease.arctic.flink.catalog.factories;

import com.netease.arctic.flink.InternalCatalogBuilder;
import com.netease.arctic.flink.catalog.ArcticCatalog;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.factories.CatalogFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.netease.arctic.flink.catalog.factories.ArcticCatalogFactoryOptions.DEFAULT_DATABASE;
import static com.netease.arctic.flink.catalog.factories.ArcticCatalogFactoryOptions.METASTORE_URL;
import static com.netease.arctic.flink.table.KafkaConnectorOptionsUtil.getKafkaParams;
import static org.apache.flink.table.factories.FactoryUtil.PROPERTY_VERSION;

/**
 * Factory for {@link ArcticCatalog}
 */
public class ArcticCatalogFactory implements CatalogFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ArcticCatalogFactory.class);

  @Override
  public String factoryIdentifier() {
    return ArcticCatalogFactoryOptions.IDENTIFIER;
  }

  @Override
  public Catalog createCatalog(Context context) {

    final FactoryUtil.CatalogFactoryHelper helper =
        FactoryUtil.createCatalogFactoryHelper(this, context);
    helper.validate();

    final String defaultDatabase = helper.getOptions().get(DEFAULT_DATABASE);
    String metastoreUrl = helper.getOptions().get(METASTORE_URL);
    final Map<String, String> arcticCatalogProperties = getKafkaParams(context.getOptions());

    return new ArcticCatalog(context.getName(), defaultDatabase,
        InternalCatalogBuilder.builder().metastoreUrl(metastoreUrl).properties(arcticCatalogProperties));
  }

  @Override
  public Set<ConfigOption<?>> requiredOptions() {
    return Collections.emptySet();
  }

  @Override
  public Set<ConfigOption<?>> optionalOptions() {
    final Set<ConfigOption<?>> options = new HashSet<>();
    options.add(PROPERTY_VERSION);
    options.add(METASTORE_URL);
    options.add(DEFAULT_DATABASE);

    // authorization config
    options.add(ArcticCatalogFactoryOptions.AUTH_AMS_CONFIGS_DISABLE);
    options.add(ArcticCatalogFactoryOptions.AUTH_METHOD);
    options.add(ArcticCatalogFactoryOptions.SIMPLE_USER_NAME);
    options.add(ArcticCatalogFactoryOptions.KEYTAB_LOGIN_USER);
    options.add(ArcticCatalogFactoryOptions.KRB5_CONF_PATH);
    options.add(ArcticCatalogFactoryOptions.KRB5_CONF_ENCODE);
    options.add(ArcticCatalogFactoryOptions.KEYTAB_PATH);
    options.add(ArcticCatalogFactoryOptions.KEYTAB_ENCODE);
    return options;
  }
}