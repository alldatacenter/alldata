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
package org.apache.drill.exec.store.hive.schema;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.schema.SchemaPlus;
import org.apache.calcite.sql.type.SqlTypeFactoryImpl;
import org.apache.drill.exec.dotdrill.View;
import org.apache.drill.exec.planner.logical.DrillViewTable;
import org.apache.drill.exec.planner.sql.SchemaUtilites;
import org.apache.drill.exec.planner.types.DrillRelDataTypeSystem;
import org.apache.drill.exec.planner.types.HiveToRelDataTypeConverter;
import org.apache.drill.exec.store.SchemaConfig;
import org.apache.drill.exec.store.hive.HiveReadEntry;
import org.apache.drill.exec.store.hive.HiveTableWithColumnCache;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;

import static java.util.stream.Collectors.toList;

/**
 * DrillViewTable which may be created from Hive view metadata and will work
 * similar to views defined in Drill.
 */
public class DrillHiveViewTable extends DrillViewTable {

  private static final HiveToRelDataTypeConverter DATA_TYPE_CONVERTER = new HiveToRelDataTypeConverter(
      new SqlTypeFactoryImpl(DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM));

  public DrillHiveViewTable(HiveReadEntry entry, List<String> schemaPath,
                            SchemaConfig schemaConfig,
                            String user) {
    super(createView(schemaPath, entry.getTable()), user, schemaConfig.getViewExpansionContext());
  }

  /**
   * Because tables used by hive views, defined without name
   * of storage plugin, we're making sure that storage plugin
   * name will be taken into account for the special case,
   * when hive storage based authorization is used, and user
   * can query view, but doesn't have rights to  access underlying
   * table.
   *
   * @param context - to rel conversion context
   * @param rowType - data type of requested columns
   * @param workspaceSchemaPath - path to view in drill, for example: ["hive"]
   * @param tokenSchemaTree - schema created for impersonated user
   * @return - relational representation of expanded Hive view
   */
  @Override
  protected RelNode expandViewForImpersonatedUser(RelOptTable.ToRelContext context, RelDataType rowType,
                                                  List<String> workspaceSchemaPath, SchemaPlus tokenSchemaTree) {
    SchemaPlus drillHiveSchema = SchemaUtilites.findSchema(tokenSchemaTree, workspaceSchemaPath);
    workspaceSchemaPath = ImmutableList.of();
    return super.expandViewForImpersonatedUser(context, rowType, workspaceSchemaPath, drillHiveSchema);
  }

  /**
   * Responsible for creation of View based on Hive view metadata.
   * Usually such instances created as a result of reading .view.drill files.
   *
   * @param schemaPath - path to view in drill, for example: ["hive"]
   * @param hiveView - hive view metadata
   * @return - View object for further usage
   */
  private static View createView(List<String> schemaPath, HiveTableWithColumnCache hiveView) {
    List<View.Field> viewFields = getViewFieldTypes(hiveView);
    String viewName = hiveView.getTableName();
    String viewSql = hiveView.getViewExpandedText();
    return new View(viewName, viewSql, viewFields, schemaPath);
  }

  /**
   * Helper method for conversion of hive view fields
   * to drill view fields
   *
   * @param hiveTable - hive view metadata
   * @return - list of fields for construction of View
   */
  private static List<View.Field> getViewFieldTypes(HiveTableWithColumnCache hiveTable) {
    return Stream.of(hiveTable.getColumnListsCache().getTableSchemaColumns(), hiveTable.getPartitionKeys())
        .flatMap(Collection::stream)
        .map(hiveField -> new View.Field(hiveField.getName(), DATA_TYPE_CONVERTER.convertToNullableRelDataType(hiveField)))
        .collect(toList());
  }

}
