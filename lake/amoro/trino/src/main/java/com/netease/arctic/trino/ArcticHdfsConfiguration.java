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

import io.trino.hdfs.HdfsConfiguration;
import io.trino.hdfs.HdfsContext;
import io.trino.spi.classloader.ThreadContextClassLoader;
import org.apache.hadoop.conf.Configuration;

import javax.inject.Inject;
import java.net.URI;

/**
 * Factory to generate Configuration of Hadoop
 */
public class ArcticHdfsConfiguration implements HdfsConfiguration {

  private ArcticCatalogFactory arcticCatalogFactory;

  @Inject
  public ArcticHdfsConfiguration(ArcticCatalogFactory arcticCatalogFactory) {
    this.arcticCatalogFactory = arcticCatalogFactory;
  }

  @Override
  public Configuration getConfiguration(HdfsContext context, URI uri) {
    try (ThreadContextClassLoader ignored = new ThreadContextClassLoader(this.getClass().getClassLoader())) {
      Configuration configuration = ((ArcticCatalogSupportTableSuffix) arcticCatalogFactory.getArcticCatalog())
          .getTableMetaStore().getConfiguration();
      return configuration;
    }
  }
}
