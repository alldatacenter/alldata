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
package org.apache.drill.exec.metastore.analyze;

import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.store.dfs.easy.EasyGroupScan;
import org.apache.drill.metastore.metadata.MetadataType;

/**
 * Implementation of {@link AnalyzeInfoProvider} for easy group scan tables.
 */
public class AnalyzeFileInfoProviderImpl extends AnalyzeFileInfoProvider {
  private final String tableTypeName;

  public AnalyzeFileInfoProviderImpl(String tableTypeName) {
    this.tableTypeName = tableTypeName;
  }

  @Override
  public boolean supportsGroupScan(GroupScan groupScan) {
    return groupScan instanceof EasyGroupScan;
  }

  @Override
  public String getTableTypeName() {
    return tableTypeName;
  }

  @Override
  public boolean supportsMetadataType(MetadataType metadataType) {
    switch (metadataType) {
      case FILE:
      case SEGMENT:
      case TABLE:
        return true;
      default:
        return false;
    }
  }
}
