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

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;

import java.util.Map;
import java.util.Set;

public class MapRDBFunctionalIndexInfo implements FunctionalIndexInfo {

  final private IndexDescriptor indexDesc;

  private boolean hasFunctionalField = false;

  // When we scan schemaPath in groupscan's columns, we check if this column(schemaPath) should be rewritten to '$N',
  // When there are more than two functions on the same column in index, CAST(a.b as INT), CAST(a.b as VARCHAR),
  // then we should map SchemaPath a.b to a set of SchemaPath, e.g. $1, $2
  private Map<SchemaPath, Set<SchemaPath>> columnToConvert;

  // map of functional index expression to destination SchemaPath e.g. $N
  private Map<LogicalExpression, LogicalExpression> exprToConvert;

  // map of SchemaPath involved in a functional field
  private Map<LogicalExpression, Set<SchemaPath>> pathsInExpr;

  private Set<SchemaPath> newPathsForIndexedFunction;

  private Set<SchemaPath> allPathsInFunction;

  public MapRDBFunctionalIndexInfo(IndexDescriptor indexDesc) {
    this.indexDesc = indexDesc;
    columnToConvert = Maps.newHashMap();
    exprToConvert = Maps.newHashMap();
    pathsInExpr = Maps.newHashMap();
    // keep the order of new paths, it may be related to the naming policy
    newPathsForIndexedFunction = Sets.newLinkedHashSet();
    allPathsInFunction = Sets.newHashSet();
    init();
  }

  private void init() {
    int count = 0;
    for (LogicalExpression indexedExpr : indexDesc.getIndexColumns()) {
      if (!(indexedExpr instanceof SchemaPath)) {
        hasFunctionalField = true;
        SchemaPath functionalFieldPath = SchemaPath.getSimplePath("$"+count);
        newPathsForIndexedFunction.add(functionalFieldPath);

        // now we handle only cast expression
        if (indexedExpr instanceof CastExpression) {
          // We handle only CAST directly on SchemaPath for now.
          SchemaPath pathBeingCasted = (SchemaPath)((CastExpression) indexedExpr).getInput();
          addTargetPathForOriginalPath(pathBeingCasted, functionalFieldPath);
          addPathInExpr(indexedExpr, pathBeingCasted);
          exprToConvert.put(indexedExpr, functionalFieldPath);
          allPathsInFunction.add(pathBeingCasted);
        }

        count++;
      }
    }
  }

  private void addPathInExpr(LogicalExpression expr, SchemaPath path) {
    if (!pathsInExpr.containsKey(expr)) {
      Set<SchemaPath> newSet = Sets.newHashSet();
      newSet.add(path);
      pathsInExpr.put(expr, newSet);
    }
    else {
      pathsInExpr.get(expr).add(path);
    }
  }

  private void addTargetPathForOriginalPath(SchemaPath origPath, SchemaPath newPath) {
    if (!columnToConvert.containsKey(origPath)) {
      Set<SchemaPath> newSet = Sets.newHashSet();
      newSet.add(newPath);
      columnToConvert.put(origPath, newSet);
    }
    else {
      columnToConvert.get(origPath).add(newPath);
    }
  }


  public boolean hasFunctional() {
    return hasFunctionalField;
  }

  public IndexDescriptor getIndexDesc() {
    return indexDesc;
  }

  /**
   * getNewPath: for an original path, return new rename '$N' path, notice there could be multiple renamed paths
   * if the there are multiple functional indexes refer original path.
   * @param path
   * @return
   */
  public SchemaPath getNewPath(SchemaPath path) {
    if (columnToConvert.containsKey(path)) {
      return columnToConvert.get(path).iterator().next();
    }
    return null;
  }

  /**
   * return a plain field path if the incoming index expression 'expr' is replaced to be a plain field
   * @param expr suppose to be an indexed expression
   * @return the renamed schemapath in index table for the indexed expression
   */
  public SchemaPath getNewPathFromExpr(LogicalExpression expr) {
    if (exprToConvert.containsKey(expr)) {
      return (SchemaPath)exprToConvert.get(expr);
    }
    return null;
  }

  /**
   * Suppose the index key has functions (rather than plain columns): CAST(a as int), CAST(b as varchar(10)),
   * then we want to maintain a mapping of the logical expression of that function to the schema path of the
   * base column involved in the function. In this example map has 2 entries:
   *   CAST(a as int)  --> 'a'
   *   CAST(b as varchar(10)) --> 'b'
   * @return the map of indexed expression --> the involved schema paths in a indexed expression
   */
  public Map<LogicalExpression, Set<SchemaPath>> getPathsInFunctionExpr() {
    return pathsInExpr;
  }


  public Map<LogicalExpression, LogicalExpression> getExprMap() {
    return exprToConvert;
  }

  public Set<SchemaPath> allNewSchemaPaths() {
    return newPathsForIndexedFunction;
  }

  public Set<SchemaPath> allPathsInFunction() {
    return allPathsInFunction;
  }

  public boolean supportEqualCharConvertToLike() {
    return true;
  }
}
