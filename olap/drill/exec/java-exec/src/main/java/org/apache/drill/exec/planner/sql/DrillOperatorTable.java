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
package org.apache.drill.exec.planner.sql;

import org.apache.calcite.sql.fun.SqlSumEmptyIsZeroAggFunction;
import org.apache.calcite.sql.validate.SqlNameMatcher;
import org.apache.drill.shaded.guava.com.google.common.collect.ArrayListMultimap;
import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.fun.SqlBetweenOperator;
import org.apache.drill.common.expression.FunctionCallFactory;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;
import org.apache.drill.exec.expr.fn.FunctionImplementationRegistry;
import org.apache.calcite.sql.SqlFunctionCategory;
import org.apache.calcite.sql.SqlIdentifier;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.SqlOperatorTable;
import org.apache.calcite.sql.SqlSyntax;
import org.apache.calcite.sql.fun.SqlStdOperatorTable;
import org.apache.drill.exec.planner.physical.PlannerSettings;
import org.apache.drill.exec.server.options.OptionManager;

import java.util.List;
import java.util.Map;


/**
 * Implementation of {@link SqlOperatorTable} that contains standard operators and functions provided through
 * {@link #inner SqlStdOperatorTable}, and Drill User Defined Functions.
 */
public class DrillOperatorTable extends SqlStdOperatorTable {
  private static final SqlOperatorTable inner = SqlStdOperatorTable.instance();
  private final List<SqlOperator> calciteOperators = Lists.newArrayList();
  private final List<SqlOperator> drillOperatorsWithoutInference = Lists.newArrayList();
  private final List<SqlOperator> drillOperatorsWithInference = Lists.newArrayList();
  private final Map<SqlOperator, SqlOperator> calciteToWrapper = Maps.newIdentityHashMap();

  private final ArrayListMultimap<String, SqlOperator> drillOperatorsWithoutInferenceMap = ArrayListMultimap.create();
  private final ArrayListMultimap<String, SqlOperator> drillOperatorsWithInferenceMap = ArrayListMultimap.create();
  // indicates remote function registry version based on which drill operator were loaded
  // is used to define if we need to reload operator table in case remote function registry version has changed
  private int functionRegistryVersion;

  private final OptionManager systemOptionManager;

  public DrillOperatorTable(FunctionImplementationRegistry registry, OptionManager systemOptionManager) {
    registry.register(this);
    calciteOperators.addAll(inner.getOperatorList());
    populateWrappedCalciteOperators();
    this.systemOptionManager = systemOptionManager;
  }

  /**
   * Set function registry version based on which operator table was loaded.
   *
   * @param version registry version
   */
  public void setFunctionRegistryVersion(int version) {
    functionRegistryVersion = version;
  }

  /**
   * @return function registry version based on which operator table was loaded
   */
  public int getFunctionRegistryVersion() {
    return functionRegistryVersion;
  }

  /**
   * When the option planner.type_inference.enable is turned off, the operators which are added via this method
   * will be used.
   */
  public void addOperatorWithoutInference(String name, SqlOperator op) {
    drillOperatorsWithoutInference.add(op);
    drillOperatorsWithoutInferenceMap.put(name.toLowerCase(), op);
  }

  /**
   * When the option planner.type_inference.enable is turned on, the operators which are added via this method
   * will be used.
   */
  public void addOperatorWithInference(String name, SqlOperator op) {
    drillOperatorsWithInference.add(op);
    drillOperatorsWithInferenceMap.put(name.toLowerCase(), op);
  }

  @Override
  public void lookupOperatorOverloads(SqlIdentifier opName, SqlFunctionCategory category,
      SqlSyntax syntax, List<SqlOperator> operatorList, SqlNameMatcher nameMatcher) {
    if (isInferenceEnabled()) {
      populateFromTypeInference(opName, category, syntax, operatorList, nameMatcher);
    } else {
      populateFromWithoutTypeInference(opName, category, syntax, operatorList, nameMatcher);
    }
  }

  private void populateFromTypeInference(SqlIdentifier opName, SqlFunctionCategory category,
                                         SqlSyntax syntax, List<SqlOperator> operatorList, SqlNameMatcher nameMatcher) {
    final List<SqlOperator> calciteOperatorList = Lists.newArrayList();
    inner.lookupOperatorOverloads(opName, category, syntax, calciteOperatorList, nameMatcher);
    if (!calciteOperatorList.isEmpty()) {
      for (SqlOperator calciteOperator : calciteOperatorList) {
        if (calciteToWrapper.containsKey(calciteOperator)) {
          operatorList.add(calciteToWrapper.get(calciteOperator));
        } else {
          operatorList.add(calciteOperator);
        }
      }
    } else {
      // if no function is found, check in Drill UDFs
      if (operatorList.isEmpty() && (syntax == SqlSyntax.FUNCTION || syntax == SqlSyntax.FUNCTION_ID) && opName.isSimple()) {
        List<SqlOperator> drillOps = drillOperatorsWithInferenceMap.get(opName.getSimple().toLowerCase());
        if (drillOps != null && !drillOps.isEmpty()) {
          operatorList.addAll(drillOps);
        }
      }
    }
  }

  private void populateFromWithoutTypeInference(SqlIdentifier opName, SqlFunctionCategory category,
                                                SqlSyntax syntax, List<SqlOperator> operatorList, SqlNameMatcher nameMatcher) {
    inner.lookupOperatorOverloads(opName, category, syntax, operatorList, nameMatcher);
    if (operatorList.isEmpty() && (syntax == SqlSyntax.FUNCTION || syntax == SqlSyntax.FUNCTION_ID) && opName.isSimple()) {
      List<SqlOperator> drillOps = drillOperatorsWithoutInferenceMap.get(opName.getSimple().toLowerCase());
      if (drillOps != null) {
        operatorList.addAll(drillOps);
      }
    }
  }

  @Override
  public List<SqlOperator> getOperatorList() {
    final List<SqlOperator> sqlOperators = Lists.newArrayList();
    sqlOperators.addAll(calciteOperators);
    if (isInferenceEnabled()) {
      sqlOperators.addAll(drillOperatorsWithInference);
    } else {
      sqlOperators.addAll(drillOperatorsWithoutInference);
    }

    return sqlOperators;
  }

  // Get the list of SqlOperator's with the given name.
  public List<SqlOperator> getSqlOperator(String name) {
    if (isInferenceEnabled()) {
      return drillOperatorsWithInferenceMap.get(name.toLowerCase());
    } else {
      return drillOperatorsWithoutInferenceMap.get(name.toLowerCase());
    }
  }

  private void populateWrappedCalciteOperators() {
    for (SqlOperator calciteOperator : inner.getOperatorList()) {
      final SqlOperator wrapper;
      if (calciteOperator instanceof SqlSumEmptyIsZeroAggFunction) {
        wrapper = new DrillCalciteSqlSumEmptyIsZeroAggFunctionWrapper(
          (SqlSumEmptyIsZeroAggFunction) calciteOperator,
          getFunctionListWithInference(calciteOperator.getName()));
      } else if (calciteOperator instanceof SqlAggFunction) {
        wrapper = new DrillCalciteSqlAggFunctionWrapper((SqlAggFunction) calciteOperator,
            getFunctionListWithInference(calciteOperator.getName()));
      } else if (calciteOperator instanceof SqlFunction) {
        wrapper = new DrillCalciteSqlFunctionWrapper((SqlFunction) calciteOperator,
            getFunctionListWithInference(calciteOperator.getName()));
      } else if (calciteOperator instanceof SqlBetweenOperator) {
        // During the procedure of converting to RexNode,
        // StandardConvertletTable.convertBetween expects the SqlOperator to be a subclass of SqlBetweenOperator
        final SqlBetweenOperator sqlBetweenOperator = (SqlBetweenOperator) calciteOperator;
        wrapper = new DrillCalciteSqlBetweenOperatorWrapper(sqlBetweenOperator);
      } else {
        final String drillOpName;
        // For UNARY_MINUS (-) or UNARY_PLUS (+), we do not rename them as function_add or function_subtract.
        // Otherwise, Calcite will mix them up with binary operator subtract (-) or add (+)
        if (calciteOperator == SqlStdOperatorTable.UNARY_MINUS || calciteOperator == SqlStdOperatorTable.UNARY_PLUS) {
          drillOpName = calciteOperator.getName();
        } else {
          drillOpName = FunctionCallFactory.convertToDrillFunctionName(calciteOperator.getName());
        }

        final List<DrillFuncHolder> drillFuncHolders = getFunctionListWithInference(drillOpName);
        if (drillFuncHolders.isEmpty()) {
          continue;
        }

        wrapper = new DrillCalciteSqlOperatorWrapper(calciteOperator, drillOpName, drillFuncHolders);
      }
      calciteToWrapper.put(calciteOperator, wrapper);
    }
  }

  private List<DrillFuncHolder> getFunctionListWithInference(String name) {
    final List<DrillFuncHolder> functions = Lists.newArrayList();
    for (SqlOperator sqlOperator : drillOperatorsWithInferenceMap.get(name.toLowerCase())) {
      if (sqlOperator instanceof DrillSqlOperator) {
        final List<DrillFuncHolder> list = ((DrillSqlOperator) sqlOperator).getFunctions();
        if (list != null) {
          functions.addAll(list);
        }
      }

      if (sqlOperator instanceof DrillSqlAggOperator) {
        final List<DrillFuncHolder> list = ((DrillSqlAggOperator) sqlOperator).getFunctions();
        if (list != null) {
          functions.addAll(list);
        }
      }
    }
    return functions;
  }

  private boolean isInferenceEnabled() {
    return systemOptionManager.getOption(PlannerSettings.TYPE_INFERENCE);
  }
}
