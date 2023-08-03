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

package com.netease.arctic.trino.iceberg;

import com.netease.arctic.catalog.ArcticCatalog;
import com.netease.arctic.trino.ArcticCatalogFactory;
import com.netease.arctic.trino.ArcticCatalogSupportTableSuffix;
import com.netease.arctic.trino.ArcticConfig;
import io.trino.spi.classloader.ThreadContextClassLoader;

import javax.inject.Inject;

public class TestArcticCatalogFactory implements ArcticCatalogFactory {

  private ArcticConfig arcticConfig;

  private ArcticCatalog arcticCatalog;

  @Inject
  public TestArcticCatalogFactory(ArcticConfig arcticConfig) {
    this.arcticConfig = arcticConfig;
  }

  //先默认只刷新一次，以后需要制定配置刷新策略ArcticCatalog
  public ArcticCatalog getArcticCatalog() {
    if (arcticCatalog == null) {
      synchronized (this) {
        if (arcticCatalog == null) {
          try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(this.getClass().getClassLoader())) {
            this.arcticCatalog =
                new ArcticCatalogSupportTableSuffix(
                    new TestBasicArcticCatalog(arcticConfig.getCatalogUrl()));
          }
        }
      }
    }
    return arcticCatalog;
  }
}
