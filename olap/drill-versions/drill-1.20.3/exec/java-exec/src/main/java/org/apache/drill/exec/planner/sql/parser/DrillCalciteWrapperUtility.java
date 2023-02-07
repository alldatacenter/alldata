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
package org.apache.drill.exec.planner.sql.parser;

import org.apache.calcite.sql.SqlAggFunction;
import org.apache.calcite.sql.SqlFunction;
import org.apache.calcite.sql.SqlOperator;
import org.apache.drill.exec.planner.sql.DrillCalciteSqlWrapper;

/**
 *
 * This utility contains the static functions to manipulate {@link DrillCalciteSqlWrapper}, {@link DrillCalciteSqlOperatorWrapper}
 * {@link DrillCalciteSqlFunctionWrapper} and {@link DrillCalciteSqlAggFunctionWrapper}.
 */
public class DrillCalciteWrapperUtility {
  /**
   * This static method will extract the SqlOperator inside the given SqlOperator if the given SqlOperator is wrapped
   * in DrillCalciteSqlWrapper and will just return the given SqlOperator if it is not wrapped.
   */
  public static SqlOperator extractSqlOperatorFromWrapper(final SqlOperator sqlOperator) {
    if(sqlOperator instanceof DrillCalciteSqlWrapper) {
      return ((DrillCalciteSqlWrapper) sqlOperator).getOperator();
    } else {
      return sqlOperator;
    }
  }

  /**
   * This static method will extract the SqlFunction inside the given SqlFunction if the given SqlFunction is wrapped
   * in DrillCalciteSqlFunctionWrapper and will just return the given SqlFunction if it is not wrapped.
   */
  public static SqlFunction extractSqlOperatorFromWrapper(final SqlFunction sqlFunction) {
    if(sqlFunction instanceof DrillCalciteSqlWrapper) {
      return (SqlFunction) ((DrillCalciteSqlWrapper) sqlFunction).getOperator();
    } else {
      return sqlFunction;
    }
  }

  /**
   * This static method will extract the SqlAggFunction inside the given SqlAggFunction if the given SqlFunction is wrapped
   * in DrillCalciteSqlAggFunctionWrapper and will just return the given SqlAggFunction if it is not wrapped.
   */
  public static SqlAggFunction extractSqlOperatorFromWrapper(final SqlAggFunction sqlAggFunction) {
    if(sqlAggFunction instanceof DrillCalciteSqlWrapper) {
      return (SqlAggFunction) ((DrillCalciteSqlWrapper) sqlAggFunction).getOperator();
    } else {
      return sqlAggFunction;
    }
  }

  /**
   * This class is not intended to be instantiated
   */
  private DrillCalciteWrapperUtility() {
  }
}
