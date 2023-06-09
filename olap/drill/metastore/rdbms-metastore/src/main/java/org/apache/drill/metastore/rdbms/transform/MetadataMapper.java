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
import org.jooq.Table;

import java.util.List;

/**
 * Provides various mapping, transformation methods for the given
 * RDBMS table and Metastore component metadata unit.
 *
 * @param <U> Metastore component metadata type
 * @param <R> RDBMS table record type
 */
public interface MetadataMapper<U, R extends Record> {

  /**
   * @return RDBMS table instance
   */
  Table<R> table();

  /**
   * @return Metastore component metadata unit instance with all fields set to null
   */
  U emptyUnit();

  /**
   * Converts RDBMS table record into Metastore component metadata unit.
   *
   * @param record RDBMS table record
   * @return Metastore component metadata unit instance
   */
  U toUnit(Record record);

  /**
   * Converts Metastore component metadata unit into RDBMS table record.
   *
   * @param unit Metastore component metadata unit
   * @return RDBMS table record instance
   */
  R toRecord(U unit);

  /**
   * Matches given list of Metastore columns to the available
   * RDBMS table columns.
   *
   * @param columns list of Metastore columns
   * @return list of RDBMS table fields
   */
  List<Field<?>> toFields(List<MetastoreColumn> columns);

  /**
   * Converts Metastore filter expression into JOOQ condition instance
   * which will be used as where clause in SQL query.
   *
   * @param filter filter expression
   * @return JOOQ condition instance
   */
  Condition toCondition(FilterExpression filter);

  /**
   * Since data in Metastore is deleted by partition, extracts
   * partitions values from given list of Metastore component metadata units
   * and creates list of delete conditions based on them.
   *
   * @param units list of Metastore component metadata units
   * @return list of JOOQ condition instances
   */
  List<Condition> toDeleteConditions(List<U> units);
}
