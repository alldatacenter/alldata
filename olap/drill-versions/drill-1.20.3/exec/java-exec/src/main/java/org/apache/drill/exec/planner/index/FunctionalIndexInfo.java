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

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;

import java.util.Map;
import java.util.Set;

/**
 * FunctionalIndexInfo is to collect Functional fields in IndexDescriptor, derive information needed for index plan,
 * e.g. convert and rewrite filter, columns, and rowtype on index scan that involve functional index.
 * In case different store might have different way to rename expression in index table, we allow storage plugin
 */
public interface FunctionalIndexInfo {

  /**
   * @return if this index has functional indexed field, return true
   */
  boolean hasFunctional();

  /**
   * @return the IndexDescriptor this IndexInfo built from
   */
  IndexDescriptor getIndexDesc();

  /**
   * getNewPath: for an original path, return new rename '$N' path, notice there could be multiple renamed paths
   * if the there are multiple functional indexes refer original path.
   * @param path
   * @return
   */
  SchemaPath getNewPath(SchemaPath path);

  /**
   * return a plain field path if the incoming index expression 'expr' is replaced to be a plain field
   * @param expr suppose to be an indexed expression
   * @return the renamed schemapath in index table for the indexed expression
   */
  SchemaPath getNewPathFromExpr(LogicalExpression expr);

  /**
   * @return the map of indexed expression --> the involved schema paths in a indexed expression
   */
  Map<LogicalExpression, Set<SchemaPath>> getPathsInFunctionExpr();

  /**
   * @return the map between indexed expression and to-be-converted target expression for scan in index
   * e.g. cast(a.b as int) -> '$0'
   */
  Map<LogicalExpression, LogicalExpression> getExprMap();

  /**
   * @return the set of all new field names for indexed functions in index
   */
  Set<SchemaPath> allNewSchemaPaths();

  /**
   * @return the set of all schemaPath exist in functional index fields
   */
  Set<SchemaPath> allPathsInFunction();

  /**
   * Whether this implementation( may be different per storage) support rewrite rewriting varchar equality expression,
   * e.g. cast(a.b as varchar(2)) = 'ca'  to LIKE expression: cast(a.b as varchar(2) LIKE 'ca%'
   */
  boolean supportEqualCharConvertToLike();

}
