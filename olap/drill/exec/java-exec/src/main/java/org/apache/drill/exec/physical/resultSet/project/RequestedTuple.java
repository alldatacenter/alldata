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
package org.apache.drill.exec.physical.resultSet.project;

import java.util.List;

import org.apache.drill.common.exceptions.CustomErrorContext;
import org.apache.drill.exec.record.metadata.ColumnMetadata;

/**
 * Represents the set of columns projected for a tuple (row or map.)
 * Each column may have structure: a set of referenced names or
 * array indices.
 * <p>
 * Three variations exist:
 * <ul>
 * <li>Project all ({@link ImpliedTupleRequest#ALL_MEMBERS}): used for a tuple
 * when all columns are projected. Example: the root tuple (the row) in
 * a <tt>SELECT *</tt> query.</li>
 * <li>Project none (also {@link ImpliedTupleRequest#NO_MEMBERS}): used when
 * no columns are projected from a tuple, such as when a map itself is
 * not projected, so none of its member columns are projected.</li>
 * <li>Project some ({@link RequestedTupleImpl}: used in the
 * <tt>SELECT a, c, e</tt> case in which the query identifies which
 * columns to project (implicitly leaving out others, such as b and
 * d in our example.)</li>
 * </ul>
 * <p>
 * The result is that each tuple (row and map) has an associated
 * projection set which the code can query to determine if a newly
 * added column is wanted (and so should have a backing vector) or
 * is unwanted (and can just receive a dummy writer.)
 * <p>
 * Wildcards will set the projection type to {@code ALL}, and will
 * be retained in the projection list. Retaining the wildcard
 * is important because multiple consumers insert columns at the
 * wildcard position. For example:<br>
 * {@code SELECT filename, *, filepath FROM ...}
 */
public interface RequestedTuple {

  public enum TupleProjectionType {
    ALL, NONE, SOME
  }

  TupleProjectionType type();
  int size();
  RequestedColumn get(int i);
  RequestedColumn get(String colName);
  boolean isProjected(String colName);
  boolean isProjected(ColumnMetadata columnSchema);
  boolean enforceProjection(ColumnMetadata columnSchema, CustomErrorContext errorContext);
  RequestedTuple mapProjection(String colName);
  List<RequestedColumn> projections();
  void buildName(StringBuilder buf);

  /**
   * Report if the projection is empty as occurs in
   * {@code SELECT COUNT(*) FROM ...}. This is <i>not</i> the
   * same as asking if this tuple is unprojected, as that concept
   * does not apply to tuples, only to the column that contains the
   * tuple.
   *
   * @return {@code true} if the projection set is empty
   */
  boolean isEmpty();
}
