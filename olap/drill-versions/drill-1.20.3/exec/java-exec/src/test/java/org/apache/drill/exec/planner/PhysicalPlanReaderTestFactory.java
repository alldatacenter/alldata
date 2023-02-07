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
package org.apache.drill.exec.planner;

import org.apache.drill.categories.PlannerTest;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.config.LogicalPlanPersistence;
import org.apache.drill.common.scanner.ClassPathScanner;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.proto.CoordinationProtos;
import org.apache.drill.exec.server.DrillbitContext;
import org.apache.drill.exec.store.StoragePluginRegistry;
import org.junit.experimental.categories.Category;

@Category(PlannerTest.class)
public class PhysicalPlanReaderTestFactory {

  public static LogicalPlanPersistence defaultLogicalPlanPersistence(DrillConfig c) {
    ScanResult scanResult = ClassPathScanner.fromPrescan(c);
    return new LogicalPlanPersistence(c, scanResult);
  }

  public static PhysicalPlanReader defaultPhysicalPlanReader(
      DrillConfig c,
      StoragePluginRegistry storageRegistry) {
    ScanResult scanResult = ClassPathScanner.fromPrescan(c);
    return new PhysicalPlanReader(
        c, scanResult, new LogicalPlanPersistence(c, scanResult),
        CoordinationProtos.DrillbitEndpoint.getDefaultInstance(),
        storageRegistry);
  }
  public static PhysicalPlanReader defaultPhysicalPlanReader(DrillConfig c) {
    return defaultPhysicalPlanReader(c, null);
  }

  public static PhysicalPlanReader defaultPhysicalPlanReader(DrillbitContext c) {
    return defaultPhysicalPlanReader(c, null);
  }

  public static PhysicalPlanReader defaultPhysicalPlanReader(
      DrillbitContext c,
      StoragePluginRegistry storageRegistry) {
    return new PhysicalPlanReader(
        c.getConfig(),
        c.getClasspathScan(),
        c.getLpPersistence(),
        CoordinationProtos.DrillbitEndpoint.getDefaultInstance(),
        storageRegistry);
  }

}
