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

package com.netease.arctic.trino;

import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.catalog.CatalogLoader;
import io.trino.spi.classloader.ThreadContextClassLoader;

import javax.inject.Inject;
import java.util.Collections;

/**
 * A factory to generate {@link ArcticCatalog}
 */
public class DefaultArcticCatalogFactory implements ArcticCatalogFactory {

  private ArcticConfig arcticConfig;

  private ArcticCatalog arcticCatalog;

  @Inject
  public DefaultArcticCatalogFactory(ArcticConfig arcticConfig) {
    this.arcticConfig = arcticConfig;
  }

  public ArcticCatalog getArcticCatalog() {
    if (arcticCatalog == null) {
      synchronized (this) {
        if (arcticCatalog == null) {
          try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(this.getClass().getClassLoader())) {
            this.arcticCatalog =
                new ArcticCatalogSupportTableSuffix(
                    CatalogLoader.load(arcticConfig.getCatalogUrl(), Collections.emptyMap()));
          }
        }
      }
    }
    return arcticCatalog;
  }
}
