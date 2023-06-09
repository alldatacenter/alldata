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

import org.apache.calcite.rel.core.TableScan;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.exec.metastore.ColumnNamesOptions;
import org.apache.drill.exec.physical.base.GroupScan;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.metastore.components.tables.BasicTablesRequests;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

/**
 * Interface for obtaining information required for analyzing tables such as table segment columns, etc.
 */
public interface AnalyzeInfoProvider {

  /**
   * Returns list of segment column names for specified {@link DrillTable} table.
   *
   * @param table              table for which should be returned segment column names
   * @param columnNamesOptions column names option values
   * @return list of segment column names
   */
  List<SchemaPath> getSegmentColumns(DrillTable table, ColumnNamesOptions columnNamesOptions) throws IOException;

  /**
   * Returns list of fields required for ANALYZE.
   *
   * @param table              drill table
   * @param metadataLevel      metadata level for analyze
   * @param columnNamesOptions column names option values
   * @return list of fields required for ANALYZE
   */
  List<SchemaPath> getProjectionFields(DrillTable table, MetadataType metadataLevel, ColumnNamesOptions columnNamesOptions) throws IOException;

  /**
   * Returns {@link MetadataInfoCollector} instance for obtaining information about segments, files, etc.
   * which should be handled in metastore.
   *
   * @param basicRequests       Metastore tables data provider helper
   * @param tableInfo           table info
   * @param selection           format selection
   * @param settings            planner settings
   * @param tableScanSupplier   supplier for table scan
   * @param interestingColumns  list of interesting columns
   * @param metadataLevel       metadata level
   * @param segmentColumnsCount number of segment columns
   * @return {@link MetadataInfoCollector} instance
   */
  MetadataInfoCollector getMetadataInfoCollector(BasicTablesRequests basicRequests, TableInfo tableInfo,
      FormatSelection selection, PlannerSettings settings, Supplier<TableScan> tableScanSupplier,
      List<SchemaPath> interestingColumns, MetadataType metadataLevel, int segmentColumnsCount) throws IOException;

  /**
   * Provides schema path to field which will be used as a location for specific table data,
   * for example, for file-based tables, it may be `fqn`.
   *
   * @param columnNamesOptions column names option values
   * @return location field
   */
  SchemaPath getLocationField(ColumnNamesOptions columnNamesOptions);

  /**
   * Returns expression which may be used to determine parent location for specific table data,
   * i.e. segment location. For example, for file-based tables, such expression will be `parentPath` function call.
   *
   * @param locationField location field
   * @return expression to determine parent location
   */
  NamedExpression getParentLocationExpression(SchemaPath locationField);

  /**
   * Checks whether this {@link AnalyzeInfoProvider} supports specified {@link GroupScan} type.
   *
   * @param groupScan group scan
   * @return {@code true} if this {@link AnalyzeInfoProvider} supports specified {@link GroupScan} type
   */
  boolean supportsGroupScan(GroupScan groupScan);

  /**
   * Returns table type name supported by this {@link AnalyzeInfoProvider}.
   *
   * @return table type name
   */
  String getTableTypeName();

  /**
   * Checks whether this {@link AnalyzeInfoProvider} supports specified {@link MetadataType}.
   *
   * @param metadataType metadata type
   * @return {@code true} if this {@link AnalyzeInfoProvider} supports specified {@link MetadataType}
   */
  boolean supportsMetadataType(MetadataType metadataType);
}
