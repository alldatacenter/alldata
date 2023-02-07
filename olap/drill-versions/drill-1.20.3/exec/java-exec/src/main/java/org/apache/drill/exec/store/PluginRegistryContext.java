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
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.server.DrillbitContext;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Provides a loose coupling of the plugin registry to the resources it needs
 * from elsewhere. Allows binding the registry via the {@code DrillbitContext}
 * in production, and to ad-hoc versions in tests.
 */
public interface PluginRegistryContext {
  DrillConfig config();
  ObjectMapper mapper();
  ObjectMapper hoconMapper();
  ScanResult classpathScan();

  // TODO: Remove this here and from StoragePlugin constructors.
  // DrillbitContext is too complex and intimate to expose to
  // extensions
  DrillbitContext drillbitContext();
}
