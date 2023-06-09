/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.drill.exec.store;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.server.DrillbitContext;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory;

/**
 * Implementation of the storage registry context which obtains the
 * needed resources from the {@code DrillbitContext}.
 */
public class DrillbitPluginRegistryContext implements PluginRegistryContext {

  private final DrillbitContext drillbitContext;
  private final ObjectMapper mapper;
  private final ObjectMapper hoconMapper;

  public DrillbitPluginRegistryContext(DrillbitContext drillbitContext) {
    this.drillbitContext = drillbitContext;

    mapper = drillbitContext.getLpPersistence().getMapper();

    // Specialized form of the persistence mechanism
    // to handle HOCON format in the override file
    LogicalPlanPersistence persistence = new LogicalPlanPersistence(drillbitContext.getConfig(),
        drillbitContext.getClasspathScan(),
        new ObjectMapper(new HoconFactory()));
    hoconMapper = persistence.getMapper();
  }

  @Override
  public DrillConfig config() {
    return drillbitContext.getConfig();
  }

  @Override
  public ObjectMapper mapper() {
    return mapper;
  }

  @Override
  public ObjectMapper hoconMapper() {
    return hoconMapper;
  }

  @Override
  public ScanResult classpathScan() {
    return drillbitContext.getClasspathScan();
  }

  @Override
  public DrillbitContext drillbitContext() {
    return drillbitContext;
  }
}
