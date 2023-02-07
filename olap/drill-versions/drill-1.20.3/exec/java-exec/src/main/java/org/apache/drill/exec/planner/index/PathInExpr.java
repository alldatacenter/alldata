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

import org.apache.drill.shaded.guava.com.google.common.collect.Sets;
import org.apache.drill.common.expression.CastExpression;
import org.apache.drill.common.expression.FunctionCall;
import org.apache.drill.common.expression.FunctionHolderExpression;
import org.apache.drill.common.expression.IfExpression;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.SchemaPath;

import org.apache.drill.common.expression.visitors.AbstractExprVisitor;

import java.util.Map;
import java.util.Set;

/**
 * Class PathInExpr is to recursively analyze a expression trees with a map of
 * indexed expression collected from indexDescriptor, e.g. Map 'cast(a.q as
 * int)' -> '$0' means the expression 'cast(a.q as int)' is named as '$0' in
 * index table.
 * <p>
 * for project expressions: cast(a.q as int), a.q, b + c, PathInExpr will get
 * remainderPath {a.q, b, c}, which is helpful to determine if q query is
 * covering. remainderPathsInFunction will be {a.q}, which will be used to
 * decide if the {a.q} in scan column and project rowtype should be removed or
 * not.
 * <p>
 * This class could be more generic to support any expression, for now it works
 * for only 'cast(schemapath as type)'.
 */
public class PathInExpr extends AbstractExprVisitor<Boolean,Void,RuntimeException> {

  //functional index fields and their involved SchemaPaths
  final private Map<LogicalExpression, Set<SchemaPath>> pathsInExpr;

  //collection of paths in all functional indexes
  private final Set<SchemaPath> allPaths;

  //the paths were found out of functional index expressions in query.
  private final Set<LogicalExpression> remainderPaths;

  //the paths in functional index fields but were found out of index functions expression in query
  private final Set<LogicalExpression> remainderPathsInFunctions;

  //constructor is provided a map of all functional expressions to the paths involved in  the expressions.
  public PathInExpr(Map<LogicalExpression, Set<SchemaPath>> pathsInExpr) {
    this.pathsInExpr = pathsInExpr;
    allPaths = Sets.newHashSet();
    remainderPaths = Sets.newHashSet();
    remainderPathsInFunctions = Sets.newHashSet();
    for(Map.Entry<LogicalExpression, Set<SchemaPath>> entry: pathsInExpr.entrySet()) {
      allPaths.addAll(entry.getValue());
    }
  }

  public Set<LogicalExpression> getRemainderPaths() {
    return remainderPaths;
  }

  public Set<LogicalExpression> getRemainderPathsInFunctions() {
    return remainderPathsInFunctions;
  }

  private boolean preProcess(LogicalExpression inExpr) {
    if (pathsInExpr.containsKey(inExpr)) {
      return true;
    }
    return false;
  }

  @Override
  public Boolean visitFunctionCall(FunctionCall call, Void value) throws RuntimeException {
    if (preProcess(call)) {
      //when it is true, we know this is exactly the indexed expression, no more deep search
      return true;
    }

    boolean bret = true;
    for (LogicalExpression arg : call.args()) {
      bret &= arg.accept(this, null);
      if(bret == false) {
        break;
      }
    }
    return bret;
  }

  @Override
  public Boolean visitFunctionHolderExpression(FunctionHolderExpression holder, Void value) throws RuntimeException {
    if (preProcess(holder)) {
      //when it is true, we know this is exactly the indexed expression, no more deep search
      return true;
    }
    for (LogicalExpression arg : holder.args) {
      arg.accept(this, null);
    }
    return null;
  }

  @Override
  public Boolean visitCastExpression(CastExpression castExpr, Void value) throws RuntimeException {
    if (preProcess(castExpr)) {
      //when it is true, we know this is exactly the indexed expression, no more deep search
      return true;
    }
    return castExpr.getInput().accept(this, null);
  }

  @Override
  public Boolean visitIfExpression(IfExpression ifExpr, Void value) throws RuntimeException {
    return (ifExpr.ifCondition.condition.accept(this, null)
        && ifExpr.ifCondition.expression.accept(this, null)
        && ifExpr.elseExpression.accept(this, null));
  }

  @Override
  public Boolean visitSchemaPath(SchemaPath path, Void value) throws RuntimeException {
    // we can come to here means this path was found out of indexed expressions,
    // so there is a path from query is not covered in functions
    remainderPaths.add(path);

    if(allPaths.contains(path)) {
      // 'path' is a path involved in a functional index field,
      remainderPathsInFunctions.add(path);

      return false;
    }
    //it is not in
    return true;
  }

  @Override
  public Boolean visitUnknown(LogicalExpression e, Void value) throws RuntimeException {
    return true;
  }
}
