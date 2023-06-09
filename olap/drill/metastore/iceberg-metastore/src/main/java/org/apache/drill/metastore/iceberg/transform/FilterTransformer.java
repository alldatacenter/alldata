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
package org.apache.drill.metastore.iceberg.transform;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.apache.drill.metastore.metadata.MetadataType;
import org.apache.iceberg.expressions.Expression;
import org.apache.iceberg.expressions.Expressions;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Transforms given input into Iceberg {@link Expression} which is used as filter
 * to retrieve, overwrite or delete Metastore component data.
 */
public class FilterTransformer {

  private static final FilterExpression.Visitor<Expression> FILTER_VISITOR = FilterExpressionVisitor.get();

  public Expression transform(FilterExpression filter) {
    return filter == null ? Expressions.alwaysTrue() : filter.accept(FILTER_VISITOR);
  }

  public Expression transform(Map<MetastoreColumn, Object> conditions) {
    if (conditions == null || conditions.isEmpty()) {
      return Expressions.alwaysTrue();
    }

    List<Expression> expressions = conditions.entrySet().stream()
      .map(entry -> Expressions.equal(entry.getKey().columnName(), entry.getValue()))
      .collect(Collectors.toList());

    if (expressions.size() == 1) {
      return expressions.get(0);
    }

    return Expressions.and(expressions.get(0), expressions.get(1),
      expressions.subList(2, expressions.size()).toArray(new Expression[0]));
  }

  public Expression transform(Set<MetadataType> metadataTypes) {
    if (metadataTypes.contains(MetadataType.ALL)) {
      return Expressions.alwaysTrue();
    }

    Set<String> inConditionValues = metadataTypes.stream()
      .map(Enum::name)
      .collect(Collectors.toSet());

    if (inConditionValues.size() == 1) {
      return Expressions.equal(MetastoreColumn.METADATA_TYPE.columnName(), inConditionValues.iterator().next());
    }

    return Expressions.in(MetastoreColumn.METADATA_TYPE.columnName(), inConditionValues);
  }

  public Expression combine(Expression... expressions) {
    if (expressions.length == 0) {
      return Expressions.alwaysTrue();
    }

    if (expressions.length == 1) {
      return expressions[0];
    }

    return Expressions.and(expressions[0], expressions[1],
      Arrays.copyOfRange(expressions, 2, expressions.length));
  }
}
