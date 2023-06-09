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
package org.apache.drill.metastore.rdbms.transform;

import org.apache.drill.metastore.MetastoreColumn;
import org.apache.drill.metastore.expressions.FilterExpression;
import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.impl.DSL;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Abstract implementation of {@link MetadataMapper} interface which contains
 * common code for all Metastore component metadata and RDBMS table types.
 *
 * @param <U> Metastore component metadata type
 * @param <R> RDBMS table record type
 */
public abstract class AbstractMetadataMapper<U, R extends Record> implements MetadataMapper<U, R> {

  @Override
  public List<Field<?>> toFields(List<MetastoreColumn> columns) {
    return columns.stream()
      .map(column -> fieldMapper().get(column))
      // ignore absent fields
      .filter(Objects::nonNull)
      .collect(Collectors.toList());
  }

  @Override
  public Condition toCondition(FilterExpression filter) {
    return filter == null ? DSL.noCondition() : filter.accept(filterVisitor());
  }

  /**
   * @return mapper specific field mapper
   */
  protected abstract Map<MetastoreColumn, Field<?>> fieldMapper();

  /**
   * @return mapper specific filter visitor
   */
  protected abstract RdbmsFilterExpressionVisitor filterVisitor();
}
