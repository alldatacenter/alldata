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
package org.apache.drill.exec.planner.index;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

import org.apache.calcite.rel.RelCollation;
import org.apache.calcite.rel.RelCollations;
import org.apache.calcite.rel.RelFieldCollation;
import org.apache.calcite.rel.RelFieldCollation.NullDirection;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DrillIndexDefinition implements IndexDefinition {
  /**
   * The indexColumns is the list of column(s) on which this index is created. If there is more than 1 column,
   * the order of the columns is important: index on {a, b} is not the same as index on {b, a}
   * NOTE: the indexed column could be of type columnfamily.column
   */
  @JsonProperty
  protected final List<LogicalExpression> indexColumns;

  /**
   * nonIndexColumns: the list of columns that are included in the index as 'covering'
   * columns but are not themselves indexed.  These are useful for covering indexes where the
   * query request can be satisfied directly by the index and avoid accessing the table altogether.
   */
  @JsonProperty
  protected final List<LogicalExpression> nonIndexColumns;

  @JsonIgnore
  protected final Set<LogicalExpression> allIndexColumns;

  @JsonProperty
  protected final List<LogicalExpression> rowKeyColumns;

  @JsonProperty
  protected final CollationContext indexCollationContext;

  /**
   * indexName: name of the index that should be unique within the scope of a table
   */
  @JsonProperty
  protected final String indexName;

  protected final String tableName;

  @JsonProperty
  protected final IndexDescriptor.IndexType indexType;

  @JsonProperty
  protected final NullDirection nullsDirection;

  public DrillIndexDefinition(List<LogicalExpression> indexCols,
                              CollationContext indexCollationContext,
                              List<LogicalExpression> nonIndexCols,
                              List<LogicalExpression> rowKeyColumns,
                              String indexName,
                              String tableName,
                              IndexType type,
                              NullDirection nullsDirection) {
    this.indexColumns = indexCols;
    this.nonIndexColumns = nonIndexCols;
    this.rowKeyColumns = rowKeyColumns;
    this.indexName = indexName;
    this.tableName = tableName;
    this.indexType = type;
    this.allIndexColumns = Sets.newHashSet(indexColumns);
    this.allIndexColumns.addAll(nonIndexColumns);
    this.indexCollationContext = indexCollationContext;
    this.nullsDirection = nullsDirection;

  }

  @Override
  public int getIndexColumnOrdinal(LogicalExpression path) {
    int id = indexColumns.indexOf(path);
    return id;
  }

  @Override
  public boolean isCoveringIndex(List<LogicalExpression> columns) {
    return allIndexColumns.containsAll(columns);
  }

  @Override
  public boolean allColumnsIndexed(Collection<LogicalExpression> columns) {
    return columnsInIndexFields(columns, indexColumns);
  }

  @Override
  public boolean someColumnsIndexed(Collection<LogicalExpression> columns) {
    return someColumnsInIndexFields(columns, indexColumns);
  }

  public boolean pathExactIn(SchemaPath path, Collection<LogicalExpression> exprs) {
    for (LogicalExpression expr : exprs) {
      if (expr instanceof SchemaPath) {
        if (((SchemaPath) expr).toExpr().equals(path.toExpr())) {
          return true;
        }
      }
    }

    return false;
  }

  boolean castIsCompatible(CastExpression castExpr, Collection<LogicalExpression> indexFields) {
    for (LogicalExpression indexExpr : indexFields) {
      if (indexExpr.getClass() != castExpr.getClass()) {
        continue;
      }
      CastExpression indexCastExpr = (CastExpression)indexExpr;
      // we compare input using equals because we know we are comparing SchemaPath,
      // if we extend to support other expression, make sure the equals of that expression
      // is implemented properly, otherwise it will fall to identity comparison
      if (!castExpr.getInput().equals(indexCastExpr.getInput()) ) {
          continue;
      }

      if (castExpr.getMajorType().getMinorType() != indexCastExpr.getMajorType().getMinorType()) {
        continue;
      }
      return true;
    }
    return false;
  }

  protected boolean columnsInIndexFields(Collection<LogicalExpression> columns, Collection<LogicalExpression> indexFields) {
    // we need to do extra check, so we could allow the case when query condition expression is not identical with indexed fields
    // and they still could use the index either by implicit cast or the difference is allowed, e.g. width of varchar
    for (LogicalExpression col : columns) {
      if (col instanceof CastExpression) {
        if (!castIsCompatible((CastExpression) col, indexFields)) {
          return false;
        }
      }
      else {
        if (!pathExactIn((SchemaPath)col, indexFields)) {
          return false;
        }
      }
    }
    return true;//indexFields.containsAll(columns);
  }

  protected boolean someColumnsInIndexFields(Collection<LogicalExpression> columns,
      Collection<LogicalExpression> indexFields) {

    // we need to do extra check, so we could allow the case when query condition expression is not identical with indexed fields
    // and they still could use the index either by implicit cast or the difference is allowed, e.g. width of varchar
    for (LogicalExpression col : columns) {
      if (col instanceof CastExpression) {
        if (castIsCompatible((CastExpression) col, indexFields)) {
          return true;
        }
      }
      else {
        if (pathExactIn((SchemaPath)col, indexFields)) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public String toString() {
    String columnsDesc = " Index columns: " + indexColumns.toString() + " Non-Index columns: " + nonIndexColumns.toString();
    String desc = "Table: " + tableName + " Index: " + indexName + columnsDesc;
    return desc;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }  else if (o == null || getClass() != o.getClass()) {
      return false;
    }

    DrillIndexDefinition index1 = (DrillIndexDefinition) o;
    return tableName.equals(index1.tableName)
        && indexName.equals(index1.indexName)
        && indexType.equals(index1.indexType)
        && indexColumns.equals(index1.indexColumns);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    final String fullName = tableName + indexName;
    int result = 1;
    result = prime * result + fullName.hashCode();
    result = prime * result + indexType.hashCode();

    return result;
  }

  @Override
  @JsonProperty
  public String getIndexName() {
    return indexName;
  }

  @Override
  public String getTableName() {
    return tableName;
  }

  @Override
  @JsonProperty
  public IndexDescriptor.IndexType getIndexType() {
    return indexType;
  }

  @Override
  @JsonProperty
  public List<LogicalExpression> getRowKeyColumns() {
    return this.rowKeyColumns;
  }

  @Override
  @JsonProperty
  public List<LogicalExpression> getIndexColumns() {
    return this.indexColumns;
  }

  @Override
  @JsonProperty
  public List<LogicalExpression> getNonIndexColumns() {
    return this.nonIndexColumns;
  }

  @Override
  @JsonIgnore
  public RelCollation getCollation() {
    if (indexCollationContext != null) {
      return RelCollations.of(indexCollationContext.relFieldCollations);
    }
    return null;
  }

  @Override
  @JsonIgnore
  public Map<LogicalExpression, RelFieldCollation> getCollationMap() {
    return indexCollationContext.collationMap;
  }

  @Override
  @JsonIgnore
  public NullDirection getNullsOrderingDirection() {
    return nullsDirection;
  }
}
