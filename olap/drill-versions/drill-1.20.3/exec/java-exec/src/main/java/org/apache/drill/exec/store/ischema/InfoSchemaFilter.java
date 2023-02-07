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
package org.apache.drill.exec.store.ischema;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;

import org.apache.drill.common.FunctionNames;
import org.apache.drill.exec.expr.fn.impl.RegexpUtil;
import org.apache.drill.exec.expr.fn.impl.RegexpUtil.SqlPatternType;
import org.apache.drill.exec.store.ischema.InfoSchemaFilter.ExprNode.Type;
import org.apache.drill.shaded.guava.com.google.common.base.Joiner;
import org.apache.drill.shaded.guava.com.google.common.base.Strings;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static org.apache.drill.exec.expr.fn.impl.RegexpUtil.sqlToRegexLike;

@JsonTypeName("info-schema-filter")
public class InfoSchemaFilter {

  private final ExprNode exprRoot;

  @JsonCreator
  public InfoSchemaFilter(@JsonProperty("exprRoot") ExprNode exprRoot) {
    this.exprRoot = exprRoot;
  }

  @JsonProperty("exprRoot")
  public ExprNode getExprRoot() {
    return exprRoot;
  }

  // include type-info to be able to deserialize subclasses correctly
  @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, property = "@class")
  public static class ExprNode {
    @JsonProperty
    public Type type;

    public ExprNode(Type type) {
      this.type = type;
    }

    public enum Type {
      FUNCTION,
      FIELD,
      CONSTANT
    }
  }

  public static class FunctionExprNode extends ExprNode {
    @JsonProperty
    public String function;

    @JsonProperty
    public List<ExprNode> args;

    @JsonCreator
    public FunctionExprNode(@JsonProperty("function") String function,
        @JsonProperty("args") List<ExprNode> args) {
      super(Type.FUNCTION);
      this.function = function;
      this.args = args;
    }

    @Override
    public String toString() {
      StringBuilder builder = new StringBuilder();
      builder.append(function);
      builder.append("(");
      builder.append(Joiner.on(",").join(args));
      builder.append(")");
      return builder.toString();
    }
  }

  public static class FieldExprNode extends ExprNode {
    @JsonProperty
    public String field;

    @JsonCreator
    public FieldExprNode(@JsonProperty("field") String field) {
      super(Type.FIELD);
      this.field = field;
    }

    @Override
    public String toString() {
      return String.format("Field=%s", field);
    }
  }

  public static class ConstantExprNode extends ExprNode {
    @JsonProperty
    public String value;

    @JsonCreator
    public ConstantExprNode(@JsonProperty("value") String value) {
      super(Type.CONSTANT);
      this.value = value;
    }

    @Override
    public String toString() {
      return String.format("Literal=%s", value);
    }
  }

  public enum Result {
    TRUE,
    FALSE,
    INCONCLUSIVE
  }

  /**
   * Evaluate the filter for given <COLUMN NAME, VALUE> pairs.
   *
   * @param recordValues map of field names and their values
   * @return evaluation result
   */
  @JsonIgnore
  public Result evaluate(Map<String, String> recordValues) {
    return evaluateHelper(recordValues, false, getExprRoot());
  }

  /**
   * Evaluate the filter for given <COLUMN NAME, VALUE> pairs.
   *
   * @param recordValues map of field names and their values
   * @param prefixMatchesInconclusive whether a prefix match between a schema path and a filter value
   *                                  results in Result.INCONCLUSIVE.  Used for pruning the schema search
   *                                  tree, e.g. "dfs" need not be recursed to find a schema of "cp.default"
   * @return evaluation result
   */
  @JsonIgnore
  public Result evaluate(Map<String, String> recordValues, boolean prefixMatchesInconclusive) {
    return evaluateHelper(recordValues, prefixMatchesInconclusive, getExprRoot());
  }

  private Result evaluateHelper(
    Map<String, String> recordValues,
    boolean prefixMatchesInconclusive,
    ExprNode exprNode
  ) {
    if (exprNode.type == Type.FUNCTION) {
      return evaluateHelperFunction(
        recordValues,
        prefixMatchesInconclusive,
        (FunctionExprNode) exprNode
      );
    }

    throw new UnsupportedOperationException(
        String.format("Unknown expression type '%s' in InfoSchemaFilter", exprNode.type));
  }

  private Result evaluateHelperFunction(
    Map<String, String> recordValues,
    boolean prefixMatchesInconclusive,
    FunctionExprNode exprNode
  ) {
    switch (exprNode.function) {
      case FunctionNames.LIKE: {
        FieldExprNode col = (FieldExprNode) exprNode.args.get(0);
        ConstantExprNode pattern = (ConstantExprNode) exprNode.args.get(1);
        ConstantExprNode escape = exprNode.args.size() > 2 ? (ConstantExprNode) exprNode.args.get(2) : null;
        final String fieldValue = recordValues.get(col.field);
        if (fieldValue == null) {
          return Result.INCONCLUSIVE;
        }

        RegexpUtil.SqlPatternInfo spi = escape == null
          ? sqlToRegexLike(pattern.value)
          : sqlToRegexLike(pattern.value, escape.value);

        if (Pattern.matches(spi.getJavaPatternString(), fieldValue)) {
          // E.g. pattern = 'dfs.%', schema path = 'dfs.tmp'.
          // The entire schema path matches.
          return Result.TRUE;
        }
        if (!prefixMatchesInconclusive) {
          // E.g. pattern = 'dfs.%', schema path = 'dfs'.
          // There may be prefix match but prefixMatchesInconclusive is false.
          return Result.FALSE;
        }
        if ((spi.getPatternType() == SqlPatternType.STARTS_WITH || spi.getPatternType() == SqlPatternType.CONSTANT) &&
          !spi.getSimplePatternString().startsWith(fieldValue)) {
            // E.g. pattern = 'dfs.%', schema path = 'cp'.
            // No match, not even to a prefix.
            return Result.FALSE;
          }
        // E.g. pattern = 'dfs.%', schema path = 'dfs'.
        // A prefix matches
        return Result.INCONCLUSIVE;
      }
      case FunctionNames.EQ:
      case "not equal": // TODO: Is this name correct?
      case "notequal":  // TODO: Is this name correct?
      case FunctionNames.NE: {
        FieldExprNode col = (FieldExprNode) exprNode.args.get(0);
        ConstantExprNode arg = (ConstantExprNode) exprNode.args.get(1);
        final String value = recordValues.get(col.field);
        if (Strings.isNullOrEmpty(value)) {
          return Result.INCONCLUSIVE;
        }

        boolean prefixMatch = arg.value.startsWith(value);
        boolean exactMatch = prefixMatch && arg.value.equals(value);

        if (exprNode.function.equals(FunctionNames.EQ)) {
          // Equality case
          if (exactMatch) {
            return Result.TRUE;
          } else {
            return prefixMatchesInconclusive && prefixMatch ? Result.INCONCLUSIVE: Result.FALSE;
          }
        } else {
          // Inequality case
          if (exactMatch) {
            return Result.FALSE;
          } else {
            return prefixMatchesInconclusive && prefixMatch ? Result.INCONCLUSIVE : Result.TRUE;
          }
        }
      }

      case FunctionNames.OR:
      case "booleanor": { // TODO: Is this name correct?
        // If at least one arg returns TRUE, then the OR function value is TRUE
        // If all args return FALSE, then OR function value is FALSE
        // For all other cases, return INCONCLUSIVE
        Result result = Result.FALSE;
        for(ExprNode arg : exprNode.args) {
          Result exprResult = evaluateHelper(recordValues, prefixMatchesInconclusive, arg);
          if (exprResult == Result.TRUE) {
            return Result.TRUE;
          } else if (exprResult == Result.INCONCLUSIVE) {
            result = Result.INCONCLUSIVE;
          }
        }

        return result;
      }

      case FunctionNames.AND:
      case "booleanand": { // TODO: Is this name correct?
        // If at least one arg returns FALSE, then the AND function value is FALSE
        // If at least one arg returns INCONCLUSIVE, then the AND function value is INCONCLUSIVE
        // If all args return TRUE, then the AND function value is TRUE
        Result result = Result.TRUE;

        for(ExprNode arg : exprNode.args) {
          Result exprResult = evaluateHelper(recordValues, prefixMatchesInconclusive, arg);
          if (exprResult == Result.FALSE) {
            return exprResult;
          }
          if (exprResult == Result.INCONCLUSIVE) {
            result = Result.INCONCLUSIVE;
          }
        }

        return result;
      }

      case "in": {
        // This case will probably only ever run if the user submits a manually
        // crafted plan because the IN operator is compiled either to a chain
        // of boolean ORs, or to a hash join with a relation which uses VALUES
        // to generate the list of constants provided to the IN operator in
        // the query. See the planner.in_subquery_threshold option.
        FieldExprNode col = (FieldExprNode) exprNode.args.get(0);
        List<ExprNode> args = exprNode.args.subList(1, exprNode.args.size());
        final String fieldValue = recordValues.get(col.field);
        if (fieldValue != null) {
          for(ExprNode arg: args) {
            if (fieldValue.equals(((ConstantExprNode) arg).value)) {
              return Result.TRUE;
            }
          }
          return Result.FALSE;
        }

        return Result.INCONCLUSIVE;
      }
    }

    throw new UnsupportedOperationException(
        String.format("Unknown function '%s' in InfoSchemaFilter", exprNode.function));
  }

  @Override
  public String toString() {
    return exprRoot.toString();
  }
}
