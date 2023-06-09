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
package org.apache.drill.exec.store.parquet;

import org.apache.drill.exec.record.metadata.ColumnMetadata;
import org.apache.drill.metastore.statistics.TableStatisticsKind;
import org.apache.drill.metastore.util.SchemaPathUtils;
import org.apache.drill.exec.record.metadata.TupleMetadata;
import org.apache.drill.exec.store.parquet.metadata.MetadataBase;
import org.apache.drill.metastore.metadata.NonInterestingColumnsMetadata;
import org.apache.drill.metastore.metadata.RowGroupMetadata;
import org.apache.drill.exec.expr.FilterBuilder;
import org.apache.drill.common.expression.ErrorCollector;
import org.apache.drill.common.expression.ErrorCollectorImpl;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;
import org.apache.drill.common.expression.visitors.AbstractExprVisitor;
import org.apache.drill.common.types.TypeProtos;
import org.apache.drill.exec.compile.sig.ConstantExpressionIdentifier;
import org.apache.drill.exec.expr.ExpressionTreeMaterializer;
import org.apache.drill.exec.expr.fn.FunctionLookupContext;
import org.apache.drill.exec.expr.stat.RowsMatch;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.ops.UdfUtilities;
import org.apache.drill.exec.server.options.OptionManager;
import org.apache.drill.metastore.statistics.ColumnStatistics;
import org.apache.drill.exec.expr.FilterPredicate;
import org.apache.drill.exec.expr.StatisticsProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class FilterEvaluatorUtils {
  private static final Logger logger = LoggerFactory.getLogger(FilterEvaluatorUtils.class);

  private FilterEvaluatorUtils() {
  }

  @SuppressWarnings("RedundantTypeArguments")
  public static RowsMatch evalFilter(LogicalExpression expr, MetadataBase.ParquetTableMetadataBase footer,
                                     int rowGroupIndex, OptionManager options, FragmentContext fragmentContext) {
    // Specifies type arguments explicitly to avoid compilation error caused by JDK-8066974
    List<SchemaPath> schemaPathsInExpr = new ArrayList<>(
            expr.<Set<SchemaPath>, Void, RuntimeException>accept(FilterEvaluatorUtils.FieldReferenceFinder.INSTANCE, null));

    RowGroupMetadata rowGroupMetadata = new ArrayList<>(ParquetTableMetadataUtils.getRowGroupsMetadata(footer).values()).get(rowGroupIndex);
    NonInterestingColumnsMetadata nonInterestingColumnsMetadata = ParquetTableMetadataUtils.getNonInterestingColumnsMeta(footer);
    Map<SchemaPath, ColumnStatistics<?>> columnsStatistics = rowGroupMetadata.getColumnsStatistics();

    // Add column statistics of non-interesting columns if there are any
    columnsStatistics.putAll(nonInterestingColumnsMetadata.getColumnsStatistics());

    columnsStatistics = ParquetTableMetadataUtils.addImplicitColumnsStatistics(columnsStatistics,
        schemaPathsInExpr, Collections.emptyList(), options, rowGroupMetadata.getPath(), true);

    return matches(expr, columnsStatistics, rowGroupMetadata.getSchema(), TableStatisticsKind.ROW_COUNT.getValue(rowGroupMetadata),
        fragmentContext, fragmentContext.getFunctionRegistry(), new HashSet<>(schemaPathsInExpr));
  }

  public static RowsMatch matches(LogicalExpression expr, Map<SchemaPath, ColumnStatistics<?>> columnsStatistics, TupleMetadata schema,
                                  long rowCount, UdfUtilities udfUtilities, FunctionLookupContext functionImplementationRegistry,
                                  Set<SchemaPath> schemaPathsInExpr) {
    ErrorCollector errorCollector = new ErrorCollectorImpl();

    LogicalExpression materializedFilter = ExpressionTreeMaterializer.materializeFilterExpr(
        expr,
        schema,
        errorCollector, functionImplementationRegistry);

    if (errorCollector.hasErrors()) {
      logger.error("{} error(s) encountered when materialize filter expression : {}",
          errorCollector.getErrorCount(), errorCollector.toErrorString());
      return RowsMatch.SOME;
    }

    Set<LogicalExpression> constantBoundaries = ConstantExpressionIdentifier.getConstantExpressionSet(materializedFilter);
    FilterPredicate<?> parquetPredicate = FilterBuilder.buildFilterPredicate(
        materializedFilter, constantBoundaries, udfUtilities, true);

    return matches(parquetPredicate, columnsStatistics, rowCount, schema, schemaPathsInExpr);
  }

  @SuppressWarnings("unchecked")
  public static <T extends Comparable<T>> RowsMatch matches(FilterPredicate<T> parquetPredicate,
                                  Map<SchemaPath, ColumnStatistics<?>> columnsStatistics,
                                  long rowCount,
                                  TupleMetadata fileMetadata,
                                  Set<SchemaPath> schemaPathsInExpr) {
    if (parquetPredicate == null) {
      return RowsMatch.SOME;
    }
    @SuppressWarnings("rawtypes")
    StatisticsProvider<T> rangeExprEvaluator = new StatisticsProvider(columnsStatistics, rowCount);
    RowsMatch rowsMatch = parquetPredicate.matches(rangeExprEvaluator);

    if (rowsMatch == RowsMatch.ALL && isMetaNotApplicable(schemaPathsInExpr, fileMetadata)) {
      rowsMatch = RowsMatch.SOME;
    }

    return rowsMatch;
  }

  private static boolean isMetaNotApplicable(Set<SchemaPath> schemaPathsInExpr, TupleMetadata fileMetadata) {
    return isRepeated(schemaPathsInExpr, fileMetadata) || isDictOrRepeatedMapChild(schemaPathsInExpr, fileMetadata);
  }

  private static boolean isRepeated(Set<SchemaPath> fields, TupleMetadata fileMetadata) {
    for (SchemaPath field : fields) {
      ColumnMetadata columnMetadata = SchemaPathUtils.getColumnMetadata(field, fileMetadata);
      TypeProtos.MajorType fieldType = columnMetadata != null ? columnMetadata.majorType() : null;
      if (fieldType != null && fieldType.getMode() == TypeProtos.DataMode.REPEATED) {
        return true;
      }
    }
    return false;
  }

  private static boolean isDictOrRepeatedMapChild(Set<SchemaPath> fields, TupleMetadata fileMetadata) {
    for (SchemaPath field : fields) {
      if (SchemaPathUtils.isFieldNestedInDictOrRepeatedMap(field, fileMetadata)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Search through a LogicalExpression, finding all internal schema path references and returning them in a set.
   */
  public static class FieldReferenceFinder extends AbstractExprVisitor<Set<SchemaPath>, Void, RuntimeException> {

    public static final FieldReferenceFinder INSTANCE = new FieldReferenceFinder();

    @Override
    public Set<SchemaPath> visitSchemaPath(SchemaPath path, Void value) {
      Set<SchemaPath> set = new HashSet<>();
      set.add(path);
      return set;
    }

    @Override
    public Set<SchemaPath> visitUnknown(LogicalExpression e, Void value) {
      Set<SchemaPath> paths = new HashSet<>();
      for (LogicalExpression ex : e) {
        paths.addAll(ex.accept(this, null));
      }
      return paths;
    }
  }
}
