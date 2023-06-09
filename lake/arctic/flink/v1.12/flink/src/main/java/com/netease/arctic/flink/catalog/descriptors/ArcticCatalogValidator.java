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

package com.netease.arctic.flink.catalog.descriptors;

import com.netease.arctic.flink.catalog.ArcticCatalog;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.table.descriptors.CatalogDescriptorValidator;
import org.apache.flink.table.descriptors.DescriptorProperties;

/**
 * Validator for {@link ArcticCatalog}
 */
public class ArcticCatalogValidator extends CatalogDescriptorValidator {
  public static final String CATALOG_TYPE_VALUE_ARCTIC = "arctic";
  public static final String METASTORE_URL = "metastore.url";
  public static final String PROPERTIES_PREFIX = "properties";

  public static final ConfigOption<String> METASTORE_URL_OPTION =
      ConfigOptions.key(METASTORE_URL).stringType().noDefaultValue();

  @Override
  public void validate(DescriptorProperties properties) {
    super.validate(properties);
    properties.validateValue(CATALOG_TYPE, CATALOG_TYPE_VALUE_ARCTIC, false);
    properties.validateString(METASTORE_URL, false);
  }
}
