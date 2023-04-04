/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.catalog;

import com.netease.arctic.ams.api.client.ArcticThriftUrl;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

public class CatalogManager {

  private String thriftAddress;
  private String currentCatalog;

  public CatalogManager(String thriftAddress) {
    ArcticThriftUrl url = ArcticThriftUrl.parse(thriftAddress);
    this.currentCatalog = url.catalogName();
    this.thriftAddress = url.serverUrl();
  }

  public ArcticCatalog getCurrentCatalog() {
    if (StringUtils.isEmpty(currentCatalog)) {
      return CatalogLoader.load(thriftAddress + currentCatalog);
    } else {
      throw new RuntimeException("The current catalog is not set");
    }
  }

  public ArcticCatalog getArcticCatalog(String catalogName) {
    ArcticCatalog catalog = CatalogLoader.load(thriftAddress + catalogName);
    this.currentCatalog = catalogName;
    return catalog;
  }

  public List<String> catalogs() {
    return CatalogLoader.catalogs(thriftAddress);
  }

  public String getThriftAddress() {
    return thriftAddress;
  }
}
