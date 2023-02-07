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
package org.apache.drill.metastore.iceberg.operate;

import org.apache.drill.metastore.operate.Metadata;
import org.apache.iceberg.Snapshot;
import org.apache.iceberg.Table;

import java.util.Map;

/**
 * Implementation of {@link Metadata} interface.
 * Provides information about current Metastore component version and its properties.
 */
public class IcebergMetadata implements Metadata {

  private final Table table;

  public IcebergMetadata(Table table) {
    this.table = table;
  }

  @Override
  public boolean supportsVersioning() {
    return true;
  }

  @Override
  public long version() {
    Snapshot snapshot = table.currentSnapshot();
    return snapshot == null ? 0 : snapshot.snapshotId();
  }

  @Override
  public Map<String, String> properties() {
    return table.properties();
  }
}
