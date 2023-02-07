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
package org.apache.drill.exec.planner.sql.handlers;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableSet;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.apache.calcite.plan.RelTraitSet;
import org.apache.calcite.rel.RelNode;
import org.apache.calcite.rel.RelShuttle;
import org.apache.calcite.rel.RelShuttleImpl;
import org.apache.calcite.rel.core.TableScan;
import org.apache.calcite.rel.logical.LogicalAggregate;
import org.apache.calcite.rel.logical.LogicalIntersect;
import org.apache.calcite.rel.logical.LogicalJoin;
import org.apache.calcite.rel.logical.LogicalMinus;
import org.apache.calcite.rel.logical.LogicalSort;
import org.apache.calcite.rel.logical.LogicalUnion;
import org.apache.calcite.rel.logical.LogicalValues;
import org.apache.calcite.rel.type.RelDataTypeField;

import org.apache.calcite.rex.RexCall;
import org.apache.calcite.rex.RexLiteral;
import org.apache.calcite.rex.RexNode;
import org.apache.calcite.rex.RexShuttle;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.type.SqlTypeName;
import org.apache.drill.common.exceptions.ExecutionSetupException;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.expr.TypeHelper;
import org.apache.drill.exec.ops.OperatorContext;
import org.apache.drill.exec.physical.base.ScanStats;
import org.apache.drill.exec.physical.impl.OutputMutator;
import org.apache.drill.exec.planner.common.DrillRelOptUtil;
import org.apache.drill.exec.planner.logical.DrillDirectScanRel;
import org.apache.drill.exec.planner.logical.DrillLimitRel;
import org.apache.drill.exec.planner.logical.DrillRel;
import org.apache.drill.exec.planner.common.DrillProjectRelBase;
import org.apache.drill.exec.planner.sql.DrillSqlOperator;
import org.apache.drill.exec.planner.sql.TypeInferenceUtils;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.store.AbstractRecordReader;
import org.apache.drill.exec.store.direct.DirectGroupScan;

import java.util.ArrayList;
import java.util.List;
import org.apache.calcite.rex.RexBuilder;
import org.apache.drill.exec.planner.common.DrillAggregateRelBase;
import org.apache.drill.exec.planner.common.DrillJoinRelBase;
import org.apache.drill.exec.planner.common.DrillUnionRelBase;
import org.apache.drill.exec.util.Pointer;

import java.math.BigDecimal;
import java.util.Set;

/**
 * Visitor that will identify whether the root portion of the RelNode tree contains a limit 0 pattern. In this case, we
 * inform the planner settings that this plan should be run as a single node plan to reduce the overhead associated with
 * executing a schema-only query.
 */
public class FindLimit0Visitor extends RelShuttleImpl {

  // Some types are excluded in this set:
  // + VARBINARY is not fully tested.
  // + MAP, ARRAY are currently not exposed to the planner.
  // + TINYINT, SMALLINT are defined in the Drill type system but have been turned off for now.
  // + SYMBOL, MULTISET, DISTINCT, STRUCTURED, ROW, OTHER, CURSOR, COLUMN_LIST are Calcite types
  //   currently not supported by Drill, nor defined in the Drill type list.
  // + ANY is the late binding type.
  private static final ImmutableSet<SqlTypeName> TYPES =
      ImmutableSet.<SqlTypeName>builder()
          .add(SqlTypeName.INTEGER, SqlTypeName.BIGINT, SqlTypeName.FLOAT, SqlTypeName.DOUBLE,
              SqlTypeName.VARCHAR, SqlTypeName.BOOLEAN, SqlTypeName.DATE, SqlTypeName.TIME,
              SqlTypeName.TIMESTAMP, SqlTypeName.INTERVAL_YEAR, SqlTypeName.INTERVAL_YEAR_MONTH,
              SqlTypeName.INTERVAL_MONTH, SqlTypeName.INTERVAL_DAY, SqlTypeName.INTERVAL_DAY_HOUR,
              SqlTypeName.INTERVAL_DAY_MINUTE, SqlTypeName.INTERVAL_DAY_SECOND, SqlTypeName.INTERVAL_HOUR,
              SqlTypeName.INTERVAL_HOUR_MINUTE, SqlTypeName.INTERVAL_HOUR_SECOND, SqlTypeName.INTERVAL_MINUTE,
              SqlTypeName.INTERVAL_MINUTE_SECOND, SqlTypeName.INTERVAL_SECOND, SqlTypeName.CHAR, SqlTypeName.DECIMAL)
          .build();

  private static final Set<String> unsupportedFunctions = ImmutableSet.<String>builder()
      // see Mappify
      .add("KVGEN")
      .add("MAPPIFY")
      // see DummyFlatten
      .add("FLATTEN")
      // see JsonConvertFrom
      .add("CONVERT_FROMJSON")
      // see JsonConvertTo class
      .add("CONVERT_TOJSON")
      .add("CONVERT_TOSIMPLEJSON")
      .add("CONVERT_TOEXTENDEDJSON")
      .build();

  private boolean contains = false;

  private FindLimit0Visitor() {
  }

  /**
   * If all field types of the given node are {@link #TYPES recognized types} and honored by execution, then this
   * method returns the tree: DrillDirectScanRel(field types). Otherwise, the method returns null.
   *
   * @param rel calcite logical rel tree
   * @return drill logical rel tree
   */
  public static DrillRel getDirectScanRelIfFullySchemaed(RelNode rel) {
    final List<RelDataTypeField> fieldList = rel.getRowType().getFieldList();
    final List<TypeProtos.MajorType> columnTypes = new ArrayList<>();

    for (final RelDataTypeField field : fieldList) {
      final SqlTypeName sqlTypeName = field.getType().getSqlTypeName();
      if (!TYPES.contains(sqlTypeName)) {
        return null;
      } else {
        final TypeProtos.MajorType.Builder builder = TypeProtos.MajorType.newBuilder()
            .setMode(field.getType().isNullable() ? TypeProtos.DataMode.OPTIONAL : TypeProtos.DataMode.REQUIRED)
            .setMinorType(TypeInferenceUtils.getDrillTypeFromCalciteType(sqlTypeName));

        if (sqlTypeName == SqlTypeName.DECIMAL) {
          builder.setScale(field.getType().getScale());
          builder.setPrecision(field.getType().getPrecision());
        } else if (TypeInferenceUtils.isScalarStringType(sqlTypeName)) {
          builder.setPrecision(field.getType().getPrecision());
        }

        columnTypes.add(builder.build());
      }
    }
    final RelTraitSet traits = rel.getTraitSet().plus(DrillRel.DRILL_LOGICAL);
    final RelDataTypeReader reader = new RelDataTypeReader(rel.getRowType().getFieldNames(), columnTypes);
    return new DrillDirectScanRel(rel.getCluster(), traits,
        new DirectGroupScan(reader, ScanStats.ZERO_RECORD_TABLE), rel.getRowType());
  }

  /**
   * Check if the root portion of the tree contains LIMIT(0).
   *
   * @param rel rel node tree
   * @return true if the root portion of the tree contains LIMIT(0)
   */
  public static boolean containsLimit0(final RelNode rel) {
    FindLimit0Visitor visitor = new FindLimit0Visitor();
    rel.accept(visitor);

    return visitor.isContains();
  }

  public static DrillRel addLimitOnTopOfLeafNodes(final DrillRel rel) {
    final Pointer<Boolean> isUnsupported = new Pointer<>(false);

    // to visit unsupported functions
    final RexShuttle unsupportedFunctionsVisitor = new RexShuttle() {
      @Override
      public RexNode visitCall(RexCall call) {
        final SqlOperator operator = call.getOperator();
        if (isUnsupportedScalarFunction(operator)) {
          isUnsupported.value = true;
          return call;
        }
        return super.visitCall(call);
      }
    };

    // to visit unsupported operators
    final RelShuttle unsupportedOperationsVisitor = new RelShuttleImpl() {
      @Override
      public RelNode visit(RelNode other) {
        if (other instanceof DrillUnionRelBase) {
          isUnsupported.value = true;
          return other;
        } else if (other instanceof DrillProjectRelBase) {
          if (!isUnsupported.value) {
            other.accept(unsupportedFunctionsVisitor);
          }
          if (isUnsupported.value) {
            return other;
          }
        }
        return super.visit(other);
      }
    };

    rel.accept(unsupportedOperationsVisitor);
    if (isUnsupported.value) {
      return rel;
    }

    // to add LIMIT (0) on top of leaf nodes
    final RelShuttle addLimitOnScanVisitor = new RelShuttleImpl() {

      private RelNode addLimitAsParent(RelNode node) {
        final RexBuilder builder = node.getCluster().getRexBuilder();
        final RexLiteral offset = builder.makeExactLiteral(BigDecimal.ZERO);
        final RexLiteral fetch = builder.makeExactLiteral(BigDecimal.ZERO);
        return new DrillLimitRel(node.getCluster(), node.getTraitSet(), node, offset, fetch);
      }

      @Override
      public RelNode visit(LogicalValues values) {
        return addLimitAsParent(values);
      }

      @Override
      public RelNode visit(TableScan scan) {
        return addLimitAsParent(scan);
      }

      @Override
      public RelNode visit(RelNode other) {
        if (other.getInputs().isEmpty()) { // leaf operator
          return addLimitAsParent(other);
        }
        return super.visit(other);
      }
    };

    return (DrillRel) rel.accept(addLimitOnScanVisitor);
  }

  private static boolean isUnsupportedScalarFunction(final SqlOperator operator) {
    return operator instanceof DrillSqlOperator &&
        unsupportedFunctions.contains(operator.getName().toUpperCase());
  }

  boolean isContains() {
    return contains;
  }

  @Override
  public RelNode visit(LogicalSort sort) {
    if (DrillRelOptUtil.isLimit0(sort.fetch)) {
      contains = true;
      return sort;
    }

    return super.visit(sort);
  }

  @Override
  public RelNode visit(RelNode other) {
    if (other instanceof DrillJoinRelBase ||
        other instanceof DrillAggregateRelBase ||
        other instanceof DrillUnionRelBase) {
      return other;
    }
    if (other instanceof DrillLimitRel) {
      if (DrillRelOptUtil.isLimit0(((DrillLimitRel) other).getFetch())) {
        contains = true;
        return other;
      }
    }

    return super.visit(other);
  }

  // The following set of RelNodes should terminate a search for the limit 0 pattern as they want convey its meaning.

  @Override
  public RelNode visit(LogicalAggregate aggregate) {
    return aggregate;
  }

  @Override
  public RelNode visit(LogicalIntersect intersect) {
    return intersect;
  }

  @Override
  public RelNode visit(LogicalJoin join) {
    return join;
  }

  @Override
  public RelNode visit(LogicalMinus minus) {
    return minus;
  }

  @Override
  public RelNode visit(LogicalUnion union) {
    return union;
  }

  /**
   * Reader for column names and types.
   */
  @JsonTypeName("RelDataTypeRecordReader")
  public static class RelDataTypeReader extends AbstractRecordReader {

    public final List<String> columnNames;
    public final List<TypeProtos.MajorType> columnTypes;

    @JsonCreator
    public RelDataTypeReader(@JsonProperty("columnNames") List<String> columnNames,
                             @JsonProperty("columnTypes") List<TypeProtos.MajorType> columnTypes) {
      Preconditions.checkArgument(columnNames.size() == columnTypes.size(), "Number of columns and their types should match");
      this.columnNames = columnNames;
      this.columnTypes = columnTypes;
    }

    @Override
    public void setup(OperatorContext context, OutputMutator output) throws ExecutionSetupException {
      for (int i = 0; i < columnNames.size(); i++) {
        final TypeProtos.MajorType type = columnTypes.get(i);
        final MaterializedField field = MaterializedField.create(columnNames.get(i), type);
        final Class vvClass = TypeHelper.getValueVectorClass(type.getMinorType(), type.getMode());
        try {
          output.addField(field, vvClass);
        } catch (SchemaChangeException e) {
          throw new ExecutionSetupException(e);
        }
      }
    }

    @Override
    public int next() {
      return 0;
    }

    @Override
    public void close() throws Exception {
    }

    /**
     * Represents RelDataTypeReader content as string, used in query plan json.
     * Example: RelDataTypeReader{columnNames=[col1], columnTypes=[INTERVALYEAR-OPTIONAL]}
     *
     * @return string representation of RelDataTypeReader content
     */
    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append("RelDataTypeReader{columnNames=");
      builder.append(columnNames).append(", columnTypes=");
      List<String> columnTypesList = new ArrayList<>(columnTypes.size());
      for (TypeProtos.MajorType columnType : columnTypes) {
        columnTypesList.add(columnType.getMinorType().toString() + "-" + columnType.getMode().toString());
      }
      builder.append(columnTypesList);
      builder.append("}");

      return builder.toString();
    }
  }
}
