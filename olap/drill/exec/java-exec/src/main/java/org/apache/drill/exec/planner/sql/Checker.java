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

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;
import org.apache.calcite.sql.SqlCallBinding;
import org.apache.calcite.sql.SqlOperandCountRange;
import org.apache.calcite.sql.SqlOperator;
import org.apache.calcite.sql.type.SqlOperandCountRanges;
import org.apache.calcite.sql.type.SqlOperandTypeChecker;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Map;

class Checker implements SqlOperandTypeChecker {
  private SqlOperandCountRange range;

  public static final Checker ANY_CHECKER = new Checker();
  private static final Map<Pair<Integer, Integer>, Checker> checkerMap = Maps.newHashMap();

  public static Checker getChecker(int min, int max) {
    final Pair<Integer, Integer> range = Pair.of(min, max);
    if(checkerMap.containsKey(range)) {
      return checkerMap.get(range);
    }

    final Checker newChecker;
    if(min == max) {
      newChecker = new Checker(min);
    } else {
      newChecker = new Checker(min, max);
    }

    checkerMap.put(range, newChecker);
    return newChecker;
  }

  private Checker(int size) {
    range = SqlOperandCountRanges.of(size);
  }

  private Checker(int min, int max) {
    range = SqlOperandCountRanges.between(min, max);
  }

  private Checker() {
    range = SqlOperandCountRanges.any();
  }

  @Override
  public boolean checkOperandTypes(SqlCallBinding callBinding, boolean throwOnFailure) {
    return true;
  }

  @Override
  public SqlOperandCountRange getOperandCountRange() {
    return range;
  }

  @Override
  public String getAllowedSignatures(SqlOperator op, String opName) {
    return opName + "(Drill - Opaque)";
  }

  @Override
  public Consistency getConsistency() {
    return Consistency.NONE;
  }

  @Override
  public boolean isOptional(int i) {
    return false;
  }

}