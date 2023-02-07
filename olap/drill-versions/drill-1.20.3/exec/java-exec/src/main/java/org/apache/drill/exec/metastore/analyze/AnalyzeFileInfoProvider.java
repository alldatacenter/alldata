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
import org.apache.drill.common.expression.ExpressionPosition;
import org.apache.drill.common.expression.FieldReference;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.logical.data.NamedExpression;
import org.apache.drill.exec.metastore.ColumnNamesOptions;
import org.apache.drill.exec.planner.logical.DrillTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.store.ColumnExplorer;
import org.apache.drill.exec.store.dfs.FileSelection;
import org.apache.drill.exec.store.dfs.FormatSelection;
import org.apache.drill.metastore.components.tables.BasicTablesRequests;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.drill.metastore.metadata.TableInfo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Implementation of {@link AnalyzeInfoProvider} for file-based tables.
 */
public abstract class AnalyzeFileInfoProvider implements AnalyzeInfoProvider {

  @Override
  public List<SchemaPath> getSegmentColumns(DrillTable table, ColumnNamesOptions columnNamesOptions) throws IOException {
    FormatSelection selection = (FormatSelection) table.getSelection();

    FileSelection fileSelection = selection.getSelection();
    if (!fileSelection.isExpandedFully()) {
      fileSelection = FileMetadataInfoCollector.getExpandedFileSelection(fileSelection);
    }

    return ColumnExplorer.getPartitionColumnNames(fileSelection, columnNamesOptions).stream()
        .map(SchemaPath::getSimplePath)
        .collect(Collectors.toList());
  }

  @Override
  public List<SchemaPath> getProjectionFields(DrillTable table, MetadataType metadataLevel, ColumnNamesOptions columnNamesOptions) throws IOException {
    List<SchemaPath> projectionList = new ArrayList<>(getSegmentColumns(table, columnNamesOptions));
    projectionList.add(SchemaPath.getSimplePath(columnNamesOptions.fullyQualifiedName()));
    projectionList.add(SchemaPath.getSimplePath(columnNamesOptions.lastModifiedTime()));
    projectionList.add(SchemaPath.getSimplePath(columnNamesOptions.projectMetadataColumn()));
    return Collections.unmodifiableList(projectionList);
  }

  @Override
  public MetadataInfoCollector getMetadataInfoCollector(BasicTablesRequests basicRequests, TableInfo tableInfo,
      FormatSelection selection, PlannerSettings settings, Supplier<TableScan> tableScanSupplier,
      List<SchemaPath> interestingColumns, MetadataType metadataLevel, int segmentColumnsCount) throws IOException {
    return new FileMetadataInfoCollector(basicRequests, tableInfo, selection,
        settings, tableScanSupplier, interestingColumns, metadataLevel, segmentColumnsCount);
  }

  @Override
  public SchemaPath getLocationField(ColumnNamesOptions columnNamesOptions) {
    return SchemaPath.getSimplePath(columnNamesOptions.fullyQualifiedName());
  }

  @Override
  public NamedExpression getParentLocationExpression(SchemaPath locationField) {
    return new NamedExpression(new FunctionCall("parentPath",
        Collections.singletonList(locationField), ExpressionPosition.UNKNOWN),
        FieldReference.getWithQuotedRef(MetastoreAnalyzeConstants.LOCATION_FIELD));
  }
}
