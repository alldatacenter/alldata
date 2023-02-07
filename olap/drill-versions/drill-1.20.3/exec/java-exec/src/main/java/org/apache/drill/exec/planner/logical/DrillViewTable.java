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
package org.apache.drill.exec.planner.logical;

import java.util.List;

import org.apache.calcite.schema.SchemaPlus;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.apache.calcite.config.CalciteConnectionConfig;
import org.apache.calcite.schema.Schema.TableType;
import org.apache.calcite.schema.Statistic;
import org.apache.calcite.schema.Statistics;
import org.apache.calcite.schema.TranslatableTable;

import org.apache.calcite.sql.SqlCall;
import org.apache.calcite.sql.SqlNode;
import org.apache.drill.exec.dotdrill.View;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.plan.RelOptTable;
import org.apache.calcite.plan.RelOptTable.ToRelContext;
import org.apache.calcite.plan.RelOptUtil;
import org.apache.calcite.rel.type.RelDataType;
import org.apache.calcite.rel.type.RelDataTypeFactory;
import org.apache.drill.exec.ops.ViewExpansionContext;

public class DrillViewTable implements TranslatableTable, DrillViewInfoProvider {
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(DrillViewTable.class);

  private final View view;
  private final String viewOwner;
  private final ViewExpansionContext viewExpansionContext;

  public DrillViewTable(View view, String viewOwner, ViewExpansionContext viewExpansionContext){
    this.view = view;
    this.viewOwner = viewOwner;
    this.viewExpansionContext = viewExpansionContext;
  }

  @Override
  public RelDataType getRowType(RelDataTypeFactory typeFactory) {
    return view.getRowType(typeFactory);
  }

  @Override
  public Statistic getStatistic() {
    return Statistics.UNKNOWN;
  }

  @Override
  public RelNode toRel(ToRelContext context, RelOptTable relOptTable) {
    ViewExpansionContext.ViewExpansionToken token = null;
    try {
      RelDataType rowType = relOptTable.getRowType();
      RelNode rel;

      if (viewExpansionContext.isImpersonationEnabled()) {
        token = viewExpansionContext.reserveViewExpansionToken(viewOwner);
        rel = expandViewForImpersonatedUser(context, rowType, view.getWorkspaceSchemaPath(), token.getSchemaTree());
      } else {
        rel = context.expandView(rowType, view.getSql(), view.getWorkspaceSchemaPath(), ImmutableList.<String>of()).rel;
      }

      // If the View's field list is not "*", create a cast.
      if (!view.isDynamic() && !view.hasStar()) {
        rel = RelOptUtil.createCastRel(rel, rowType, true);
      }

      return rel;
    } finally {
      if (token != null) {
        token.release();
      }
    }
  }


  protected RelNode expandViewForImpersonatedUser(ToRelContext context,
                                                  RelDataType rowType,
                                                  List<String> workspaceSchemaPath,
                                                  SchemaPlus tokenSchemaTree) {
    return context.expandView(rowType, view.getSql(), tokenSchemaTree, workspaceSchemaPath).rel;
  }

  @Override
  public TableType getJdbcTableType() {
    return TableType.VIEW;
  }

  @Override
  public String getViewSql() {
    return view.getSql();
  }

  @Override
  public boolean rolledUpColumnValidInsideAgg(String column,
      SqlCall call, SqlNode parent, CalciteConnectionConfig config) {
    return true;
  }

  @Override public boolean isRolledUp(String column) {
    return false;
  }

}
