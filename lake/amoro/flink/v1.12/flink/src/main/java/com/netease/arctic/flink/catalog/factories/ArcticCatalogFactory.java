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


import com.netease.arctic.ams.api.properties.CatalogMetaProperties;
import com.netease.arctic.flink.InternalCatalogBuilder;
import com.netease.arctic.flink.catalog.ArcticCatalog;
import com.netease.arctic.flink.catalog.descriptors.ArcticCatalogValidator;
import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.CatalogFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.netease.arctic.flink.catalog.descriptors.ArcticCatalogValidator.METASTORE_URL;
import static com.netease.arctic.flink.catalog.descriptors.ArcticCatalogValidator.PROPERTIES_PREFIX;
import static org.apache.flink.table.descriptors.CatalogDescriptorValidator.CATALOG_DEFAULT_DATABASE;
import static org.apache.flink.table.descriptors.CatalogDescriptorValidator.CATALOG_PROPERTY_VERSION;
import static org.apache.flink.table.descriptors.CatalogDescriptorValidator.CATALOG_TYPE;

/**
 * Factory for {@link ArcticCatalog}
 */
public class ArcticCatalogFactory implements CatalogFactory {

  private static final Logger LOG = LoggerFactory.getLogger(ArcticCatalogFactory.class);

  @Override
  public Catalog createCatalog(String name, Map<String, String> properties) {
    final DescriptorProperties descriptorProperties = getValidatedProperties(properties);

    final String defaultDatabase = descriptorProperties.getOptionalString(CATALOG_DEFAULT_DATABASE)
        .orElse(ArcticCatalog.DEFAULT_DB);
    String metastoreUrl = descriptorProperties.getString(METASTORE_URL);
    Map<String, String> arcticCatalogProperties = descriptorProperties.getPropertiesWithPrefix(PROPERTIES_PREFIX);

    return new ArcticCatalog(name, defaultDatabase,
        InternalCatalogBuilder.builder().metastoreUrl(metastoreUrl).properties(arcticCatalogProperties));
  }

  private static DescriptorProperties getValidatedProperties(Map<String, String> properties) {
    final DescriptorProperties descriptorProperties = new DescriptorProperties(true);
    descriptorProperties.putProperties(properties);

    new ArcticCatalogValidator().validate(descriptorProperties);

    return descriptorProperties;
  }

  @Override
  public Map<String, String> requiredContext() {
    Map<String, String> context = new HashMap<>();
    context.put(CATALOG_TYPE, ArcticCatalogValidator.CATALOG_TYPE_VALUE_ARCTIC);
    // backwards compatibility
    context.put(CATALOG_PROPERTY_VERSION, "1");
    return context;
  }

  @Override
  public List<String> supportedProperties() {
    List<String> properties = new ArrayList<>();

    // default database
    properties.add(CATALOG_DEFAULT_DATABASE);
    properties.add(METASTORE_URL);

    // storage config and authorization config
    properties.add(PROPERTIES_PREFIX + "." + CatalogMetaProperties.LOAD_AUTH_FROM_AMS);
    properties.add(PROPERTIES_PREFIX + "." + CatalogMetaProperties.AUTH_CONFIGS_KEY_HADOOP_USERNAME);
    properties.add(PROPERTIES_PREFIX + "." + CatalogMetaProperties.AUTH_CONFIGS_KEY_PRINCIPAL);
    properties.add(PROPERTIES_PREFIX + "." + CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB_PATH);
    properties.add(PROPERTIES_PREFIX + "." + CatalogMetaProperties.AUTH_CONFIGS_KEY_KRB_ENCODE);
    properties.add(PROPERTIES_PREFIX + "." + CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB_PATH);
    properties.add(PROPERTIES_PREFIX + "." + CatalogMetaProperties.AUTH_CONFIGS_KEY_KEYTAB_ENCODE);
    properties.add(PROPERTIES_PREFIX + "." + CatalogMetaProperties.AUTH_CONFIGS_KEY_TYPE);
    return properties;
  }
}