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

import com.netease.arctic.trino.keyed.KeyedConnectorMetadata;
import com.netease.arctic.trino.unkeyed.IcebergMetadata;
import io.airlift.json.JsonCodec;
import io.trino.plugin.hive.HdfsEnvironment;
import io.trino.plugin.iceberg.CommitTaskData;
import io.trino.plugin.iceberg.catalog.TrinoCatalogFactory;
import io.trino.spi.type.TypeManager;

import javax.inject.Inject;

import static java.util.Objects.requireNonNull;

/**
 * A factory to generate {@link ArcticConnectorMetadata}
 */
public class ArcticMetadataFactory {
  private final TypeManager typeManager;
  private final JsonCodec<CommitTaskData> commitTaskCodec;
  private final HdfsEnvironment hdfsEnvironment;
  private final ArcticCatalogFactory arcticCatalogFactory;
  private final TrinoCatalogFactory arcticTrinoCatalogFactory;

  @Inject
  public ArcticMetadataFactory(
      TypeManager typeManager,
      JsonCodec<CommitTaskData> commitTaskCodec,
      HdfsEnvironment hdfsEnvironment,
      ArcticCatalogFactory arcticCatalogFactory,
      TrinoCatalogFactory arcticTrinoCatalogFactory) {
    this.typeManager = requireNonNull(typeManager, "typeManager is null");
    this.commitTaskCodec = requireNonNull(commitTaskCodec, "commitTaskCodec is null");
    this.hdfsEnvironment = requireNonNull(hdfsEnvironment, "hdfsEnvironment is null");
    this.arcticCatalogFactory = arcticCatalogFactory;
    this.arcticTrinoCatalogFactory = arcticTrinoCatalogFactory;
  }

  public ArcticConnectorMetadata create() {
    IcebergMetadata icebergMetadata = new IcebergMetadata(typeManager, commitTaskCodec,
        arcticTrinoCatalogFactory.create(null), hdfsEnvironment);
    KeyedConnectorMetadata arcticConnectorMetadata =
        new KeyedConnectorMetadata(arcticCatalogFactory.getArcticCatalog(), typeManager);
    return new ArcticConnectorMetadata(
        arcticConnectorMetadata,
        icebergMetadata,
        arcticCatalogFactory.getArcticCatalog());
  }
}
