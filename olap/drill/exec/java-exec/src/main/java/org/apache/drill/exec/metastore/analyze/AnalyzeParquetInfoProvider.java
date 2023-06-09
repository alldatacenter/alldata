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

import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.exec.metastore.ColumnNamesOptions;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.store.parquet.ParquetGroupScan;
import org.apache.drill.metastore.metadata.MetadataType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Implementation of {@link AnalyzeInfoProvider} for parquet tables.
 */
public class AnalyzeParquetInfoProvider extends AnalyzeFileInfoProvider {
  public static final AnalyzeParquetInfoProvider INSTANCE = new AnalyzeParquetInfoProvider();

  public static final String TABLE_TYPE_NAME = "PARQUET";

  @Override
  public List<SchemaPath> getProjectionFields(DrillTable table, MetadataType metadataLevel, ColumnNamesOptions columnNamesOptions) throws IOException {
    List<SchemaPath> columnList = new ArrayList<>(super.getProjectionFields(table, metadataLevel, columnNamesOptions));
    if (metadataLevel.includes(MetadataType.ROW_GROUP)) {
      columnList.add(SchemaPath.getSimplePath(columnNamesOptions.rowGroupIndex()));
      columnList.add(SchemaPath.getSimplePath(columnNamesOptions.rowGroupStart()));
      columnList.add(SchemaPath.getSimplePath(columnNamesOptions.rowGroupLength()));
    }
    return Collections.unmodifiableList(columnList);
  }

  @Override
  public boolean supportsGroupScan(GroupScan groupScan) {
    return groupScan instanceof ParquetGroupScan;
  }

  @Override
  public String getTableTypeName() {
    return TABLE_TYPE_NAME;
  }

  @Override
  public boolean supportsMetadataType(MetadataType metadataType) {
    switch (metadataType) {
      case ROW_GROUP:
      case FILE:
      case SEGMENT:
      case TABLE:
        return true;
      default:
        return false;
    }
  }
}
