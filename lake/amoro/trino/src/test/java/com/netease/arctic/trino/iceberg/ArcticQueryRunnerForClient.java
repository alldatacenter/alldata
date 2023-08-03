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

import com.google.common.collect.ImmutableMap;
import com.netease.arctic.trino.ArcticPlugin;
import io.airlift.log.Logger;
import io.airlift.log.Logging;
import io.trino.Session;
import io.trino.testing.DistributedQueryRunner;

import java.nio.file.Path;
import java.util.Map;

import static io.trino.testing.TestingSession.testSessionBuilder;

public final class ArcticQueryRunnerForClient {
  private static final Logger log = Logger.get(ArcticQueryRunnerForClient.class);

  public static final String ARCTIC_CATALOG = "arctic";

  private ArcticQueryRunnerForClient() {
  }

  public static DistributedQueryRunner createIcebergQueryRunner(Map<String, String> extraProperties, String url) throws Exception {
    return createIcebergQueryRunner(extraProperties, false, url);
  }

  public static DistributedQueryRunner createIcebergQueryRunner(
      Map<String, String> extraProperties,
      boolean createTpchTables, String url)
      throws Exception {
    Session session = testSessionBuilder()
        .setCatalog(ARCTIC_CATALOG)
        .build();

    DistributedQueryRunner queryRunner = DistributedQueryRunner.builder(session)
        .setExtraProperties(extraProperties)
        .setNodeCount(1)
        .build();

    Path dataDir = queryRunner.getCoordinator().getBaseDataDir().resolve("arctic_data");
    Path catalogDir = dataDir.getParent().resolve("catalog");

    queryRunner.installPlugin(new ArcticPlugin());
    Map<String, String> icebergProperties = ImmutableMap.<String, String>builder()
        .put("arctic.url", url)
        .build();

    queryRunner.createCatalog(ARCTIC_CATALOG, "arctic", icebergProperties);

    return queryRunner;
  }

  public static void main(String[] args)
      throws Exception {
    Logging.initialize();
    Map<String, String> properties = ImmutableMap.of("http-server.http.port", "8080");
    String url = args[0];
    if (url == null) {
      throw new RuntimeException("please config arctic url");
    }
    DistributedQueryRunner queryRunner = null;
    queryRunner = createIcebergQueryRunner(properties, url);
    Thread.sleep(10);
    Logger log = Logger.get(ArcticQueryRunnerForClient.class);
    log.info("======== SERVER STARTED ========");
    log.info("\n====\n%s\n====", queryRunner.getCoordinator().getBaseUrl());
  }
}
