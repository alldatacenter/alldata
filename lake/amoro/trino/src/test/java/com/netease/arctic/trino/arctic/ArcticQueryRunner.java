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

package com.netease.arctic.trino.arctic;

import com.google.common.collect.ImmutableMap;
import com.netease.arctic.trino.ArcticPlugin;
import io.airlift.log.Logger;
import io.trino.plugin.tpch.TpchPlugin;
import io.trino.testing.DistributedQueryRunner;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static io.airlift.testing.Closeables.closeAllSuppress;
import static io.trino.testing.TestingSession.testSessionBuilder;
import static java.util.Objects.requireNonNull;

public final class ArcticQueryRunner {
  private static final Logger log = Logger.get(ArcticQueryRunner.class);

  public static final String ARCTIC_CATALOG = "arctic";

  private ArcticQueryRunner() {
  }

  public static Builder builder() {
    return new Builder();
  }

  public static class Builder
      extends DistributedQueryRunner.Builder<Builder> {
    private Optional<File> metastoreDirectory = Optional.empty();
    private ImmutableMap.Builder<String, String> icebergProperties = ImmutableMap.builder();

    protected Builder() {
      super(testSessionBuilder()
          .setCatalog(ARCTIC_CATALOG)
          .setSchema("tpch")
          .build());
    }

    public Builder setMetastoreDirectory(File metastoreDirectory) {
      this.metastoreDirectory = Optional.of(metastoreDirectory);
      return self();
    }

    public Builder setIcebergProperties(Map<String, String> icebergProperties) {
      this.icebergProperties = ImmutableMap.<String, String>builder()
          .putAll(requireNonNull(icebergProperties, "icebergProperties is null"));
      return self();
    }

    public Builder addIcebergProperty(String key, String value) {
      this.icebergProperties.put(key, value);
      return self();
    }

    @Override
    public DistributedQueryRunner build()
        throws Exception {
      DistributedQueryRunner queryRunner = super.build();
      try {
        queryRunner.installPlugin(new TpchPlugin());
        queryRunner.createCatalog("tpch", "tpch");

        queryRunner.installPlugin(new ArcticPlugin());
        Map<String, String> icebergProperties = new HashMap<>(this.icebergProperties.buildOrThrow());
        queryRunner.createCatalog(ARCTIC_CATALOG, "arctic", icebergProperties);
        return queryRunner;
      } catch (Exception e) {
        closeAllSuppress(e, queryRunner);
        throw e;
      }
    }
  }

  public static void main(String[] args)
      throws Exception {
    DistributedQueryRunner queryRunner = null;
    queryRunner = ArcticQueryRunner.builder()
        .setExtraProperties(ImmutableMap.of("http-server.http.port", "8080"))
        .build();
    Thread.sleep(10);
    Logger log = Logger.get(ArcticQueryRunner.class);
    log.info("======== SERVER STARTED ========");
    log.info("\n====\n%s\n====", queryRunner.getCoordinator().getBaseUrl());
  }
}
