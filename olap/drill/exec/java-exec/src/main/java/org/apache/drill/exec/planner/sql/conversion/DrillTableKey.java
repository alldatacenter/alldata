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
package org.apache.drill.exec.planner.sql.conversion;

import java.util.List;
import java.util.Objects;

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.metastore.MetadataProviderManager;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.shaded.guava.com.google.common.cache.LoadingCache;

/**
 * Key for storing / obtaining {@link MetadataProviderManager} instance from {@link LoadingCache}.
 */
final class DrillTableKey {
  private final SchemaPath key;
  private final DrillTable drillTable;

  private DrillTableKey(SchemaPath key, DrillTable drillTable) {
    this.key = key;
    this.drillTable = drillTable;
  }

  static DrillTableKey of(List<String> names, DrillTable drillTable) {
    return new DrillTableKey(SchemaPath.getCompoundPath(names.toArray(new String[0])), drillTable);
  }

  @Override
  public boolean equals(Object obj) {
    return this == obj || (obj instanceof DrillTableKey
        && Objects.equals(key, ((DrillTableKey) obj).key));
  }

  @Override
  public int hashCode() {
    return key != null ? key.hashCode() : 0;
  }

  MetadataProviderManager getMetadataProviderManager() {
    return drillTable.getMetadataProviderManager();
  }
}
