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
package org.apache.drill.exec.resolver;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.expression.MajorTypeInLogicalExpression;
import org.apache.drill.common.types.TypeProtos.DataMode;
import org.apache.drill.common.types.TypeProtos.MajorType;
import org.apache.drill.common.types.TypeProtos.MinorType;
import org.apache.drill.exec.expr.annotations.FunctionTemplate.NullHandling;
import org.apache.drill.exec.expr.fn.DrillFuncHolder;

import org.apache.drill.shaded.guava.com.google.common.collect.Lists;
import org.apache.drill.shaded.guava.com.google.common.collect.Sets;

import static org.apache.drill.exec.planner.types.DrillRelDataTypeSystem.DRILL_REL_DATATYPE_SYSTEM;

public class TypeCastRules {

  private static Map<MinorType, Set<MinorType>> rules;

  public TypeCastRules() {
  }

  static {
    initTypeRules();
  }

  private static void initTypeRules() {
    rules = new HashMap<>();

    Set<MinorType> rule;

    /** TINYINT cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.TINYINT, rule);

    /** SMALLINT cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.SMALLINT, rule);

    /** INT cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.INT, rule);

    /** BIGINT cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.BIGINT, rule);

    /** UINT4 cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.UINT4, rule);

    /** UINT8 cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.UINT8, rule);

    /** DECIMAL9 cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.VARDECIMAL);
    rules.put(MinorType.DECIMAL9, rule);

    /** DECIMAL18 cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.VARDECIMAL);
    rules.put(MinorType.DECIMAL18, rule);

    /** DECIMAL28Dense cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.VARDECIMAL);
    rules.put(MinorType.DECIMAL28DENSE, rule);

    /** DECIMAL28Sparse cast able from **/

    rule = new HashSet<>();
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.VARDECIMAL);
    rules.put(MinorType.DECIMAL28SPARSE, rule);

    /* VARDECIMAL cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.VARDECIMAL);
    rules.put(MinorType.VARDECIMAL, rule);

    /** DECIMAL38Dense cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rules.put(MinorType.DECIMAL38DENSE, rule);


    /** DECIMAL38Sparse cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.VARDECIMAL);
    rules.put(MinorType.DECIMAL38SPARSE, rule);

    /** MONEY cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.MONEY, rule);

    /** DATE cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.DATE, rule);

    /** TIME cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TIME);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.TIME, rule);

    /** TIMESTAMP cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VARBINARY);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIMESTAMPTZ);
    rules.put(MinorType.TIMESTAMP, rule);

    /** TIMESTAMPTZ cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIME);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.TIMESTAMPTZ, rule);

    /** Interval cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.INTERVAL);
    rule.add(MinorType.INTERVALDAY);
    rule.add(MinorType.INTERVALYEAR);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.INTERVAL, rule);

    /** INTERVAL YEAR cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.INTERVALYEAR);
    rule.add(MinorType.INTERVAL);
    rule.add(MinorType.INTERVALDAY);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.INTERVALYEAR, rule);

    /** INTERVAL DAY cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.INTERVALDAY);
    rule.add(MinorType.INTERVALYEAR);
    rule.add(MinorType.INTERVAL);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.INTERVALDAY, rule);

    /** FLOAT4 cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rules.put(MinorType.FLOAT4, rule);

    /** FLOAT8 cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rules.put(MinorType.FLOAT8, rule);

    /** BIT cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rules.put(MinorType.BIT, rule);

    /** FIXEDCHAR cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIME);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.INTERVAL);
    rule.add(MinorType.INTERVALYEAR);
    rule.add(MinorType.INTERVALDAY);
    rules.put(MinorType.FIXEDCHAR, rule);

    /** FIXED16CHAR cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIME);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.INTERVAL);
    rule.add(MinorType.INTERVALYEAR);
    rule.add(MinorType.INTERVALDAY);
    rules.put(MinorType.FIXED16CHAR, rule);

    /** FIXEDBINARY cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rule.add(MinorType.FIXEDBINARY);
    rules.put(MinorType.FIXEDBINARY, rule);

    /** VARCHAR cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIME);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.INTERVAL);
    rule.add(MinorType.INTERVALYEAR);
    rule.add(MinorType.INTERVALDAY);
    rules.put(MinorType.VARCHAR, rule);

    /** VAR16CHAR cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VAR16CHAR);
    rule.add(MinorType.VARBINARY);
    rule.add(MinorType.FIXEDBINARY);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIME);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.INTERVAL);
    rule.add(MinorType.INTERVALYEAR);
    rule.add(MinorType.INTERVALDAY);
    rules.put(MinorType.VAR16CHAR, rule);

    /** VARBINARY cast able from **/
    rule = new HashSet<>();
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.DECIMAL9);
    rule.add(MinorType.DECIMAL18);
    rule.add(MinorType.DECIMAL28SPARSE);
    rule.add(MinorType.DECIMAL28DENSE);
    rule.add(MinorType.DECIMAL38SPARSE);
    rule.add(MinorType.DECIMAL38DENSE);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.VARBINARY);
    rule.add(MinorType.FIXEDBINARY);
    rules.put(MinorType.VARBINARY, rule);

    rules.put(MinorType.MAP, Sets.newHashSet(MinorType.MAP));
    rules.put(MinorType.LIST, Sets.newHashSet(MinorType.LIST));
    rules.put(MinorType.UNION, Sets.newHashSet(MinorType.UNION));
    rules.put(MinorType.DICT, Sets.newHashSet(MinorType.DICT));
  }

  public static boolean isCastableWithNullHandling(MajorType argumentType, MajorType paramType, NullHandling nullHandling) {
    if ((argumentType.getMode() == DataMode.REPEATED || paramType.getMode() == DataMode.REPEATED) && argumentType.getMode() != paramType.getMode()) {
      return false;
    }
    // allow using functions with nullable parameters for required arguments only for the case of data mode mismatch
    if (nullHandling == NullHandling.INTERNAL && argumentType.getMode() != paramType.getMode()
        && (paramType.getMode() != DataMode.OPTIONAL || argumentType.getMode() != DataMode.REQUIRED)) {
      return false;
    }
    return isCastable(argumentType.getMinorType(), paramType.getMinorType());
  }

  public static boolean isCastable(MinorType from, MinorType to) {
    return from.equals(MinorType.NULL) ||      //null could be casted to any other type.
        (rules.get(to) != null && rules.get(to).contains(from));
  }

  public static DataMode getLeastRestrictiveDataMode(List<DataMode> dataModes) {
    boolean hasOptional = false;
    for(DataMode dataMode : dataModes) {
      switch (dataMode) {
        case REPEATED:
          return dataMode;
        case OPTIONAL:
          hasOptional = true;
      }
    }

    if(hasOptional) {
      return DataMode.OPTIONAL;
    } else {
      return DataMode.REQUIRED;
    }
  }

  /*
   * Function checks if casting is allowed from the 'from' -> 'to' minor type. If its allowed
   * we also check if the precedence map allows such a cast and return true if both cases are satisfied
   */
  public static MinorType getLeastRestrictiveType(List<MinorType> types) {
    assert types.size() >= 2;
    MinorType result = types.get(0);
    if (result == MinorType.UNION) {
      return result;
    }
    int resultPrec = ResolverTypePrecedence.precedenceMap.get(result);

    for (int i = 1; i < types.size(); i++) {
      MinorType next = types.get(i);
      if (next == MinorType.UNION) {
        return next;
      }
      if (next == result) {
        // both args are of the same type; continue
        continue;
      }

      int nextPrec = ResolverTypePrecedence.precedenceMap.get(next);

      if (isCastable(next, result) && resultPrec >= nextPrec) {
        // result is the least restrictive between the two args; nothing to do continue
        continue;
      } else if(isCastable(result, next) && nextPrec >= resultPrec) {
        result = next;
        resultPrec = nextPrec;
      } else {
        return null;
      }
    }

    return result;
  }

  private static final int DATAMODE_CAST_COST = 1;
  private static final int VARARG_COST = Integer.MAX_VALUE / 2;

  /**
   * Decide whether it's legal to do implicit cast. -1 : not allowed for
   * implicit cast > 0: cost associated with implicit cast. ==0: params are
   * exactly same type of arg. No need of implicit.
   */
  public static int getCost(List<MajorType> argumentTypes, DrillFuncHolder holder) {
    int cost = 0;

    if (argumentTypes.size() != holder.getParamCount() && !holder.isVarArg()) {
      return -1;
    }

    // Indicates whether we used secondary cast rules
    boolean secondaryCast = false;

    // number of arguments that could implicitly casts using precedence map or
    // didn't require casting at all
    int nCasts = 0;

    /*
     * If we are determining function holder for decimal data type, we need to
     * make sure the output type of the function can fit the precision that we
     * need based on the input types.
     */
    if (holder.checkPrecisionRange()) {
      List<LogicalExpression> logicalExpressions = Lists.newArrayList();
      for(MajorType majorType : argumentTypes) {
        logicalExpressions.add(
            new MajorTypeInLogicalExpression(majorType));
      }

      if (DRILL_REL_DATATYPE_SYSTEM.getMaxNumericPrecision() <
          holder.getReturnType(logicalExpressions).getPrecision()) {
        return -1;
      }
    }

    final int numOfArgs = argumentTypes.size();
    for (int i = 0; i < numOfArgs; i++) {
      final MajorType argType = argumentTypes.get(i);
      final MajorType paramType = holder.getParamMajorType(i);

      //@Param FieldReader will match any type
      if (holder.isFieldReader(i)) {
//        if (Types.isComplex(call.args.get(i).getMajorType()) ||Types.isRepeated(call.args.get(i).getMajorType()) )
        // add the max cost when encountered with a field reader considering
        // that it is the most expensive factor contributing to the cost.
        cost += ResolverTypePrecedence.MAX_IMPLICIT_CAST_COST;
        continue;
      }

      if (!TypeCastRules.isCastableWithNullHandling(argType, paramType, holder.getNullHandling())) {
        return -1;
      }

      Integer paramVal = ResolverTypePrecedence.precedenceMap.get(paramType
          .getMinorType());
      Integer argVal = ResolverTypePrecedence.precedenceMap.get(argType
          .getMinorType());

      if (paramVal == null) {
        throw new RuntimeException(String.format(
            "Precedence for type %s is not defined", paramType.getMinorType().name()));
      }

      if (argVal == null) {
        throw new RuntimeException(String.format(
            "Precedence for type %s is not defined", argType.getMinorType().name()));
      }

      if (paramVal - argVal < 0) {

        /* Precedence rules do not allow to implicit cast, however check
         * if the secondary rules allow us to cast
         */
        Set<MinorType> rules;
        if ((rules = (ResolverTypePrecedence.secondaryImplicitCastRules.get(paramType.getMinorType()))) != null
            && rules.contains(argType.getMinorType())) {
          secondaryCast = true;
        } else {
          return -1;
        }
      }
      // Check null vs non-null, using same logic as that in Types.softEqual()
      // Only when the function uses NULL_IF_NULL, nullable and non-nullable are interchangeable.
      // Otherwise, the function implementation is not a match.
      if (argType.getMode() != paramType.getMode()) {
        // TODO - this does not seem to do what it is intended to
//        if (!((holder.getNullHandling() == NullHandling.NULL_IF_NULL) &&
//            (argType.getMode() == DataMode.OPTIONAL ||
//             argType.getMode() == DataMode.REQUIRED ||
//             paramType.getMode() == DataMode.OPTIONAL ||
//             paramType.getMode() == DataMode.REQUIRED )))
//          return -1;
        // if the function is designed to take optional with custom null handling, and a required
        // is being passed, increase the cost to account for a null check
        // this allows for a non-nullable implementation to be preferred
        if (holder.getNullHandling() == NullHandling.INTERNAL) {
          // a function that expects required output, but nullable was provided
          if (paramType.getMode() == DataMode.REQUIRED && argType.getMode() == DataMode.OPTIONAL) {
            return -1;
          } else if (paramType.getMode() == DataMode.OPTIONAL && argType.getMode() == DataMode.REQUIRED) {
            cost+= DATAMODE_CAST_COST;
          }
        }
      }

      int castCost;

      if ((castCost = (paramVal - argVal)) >= 0) {
        nCasts++;
        cost += castCost;
      }
    }

    if (holder.isVarArg()) {
      int varArgIndex = holder.getParamCount() - 1;
      for (int i = varArgIndex; i < numOfArgs; i++) {
        if (holder.isFieldReader(varArgIndex)) {
          break;
        } else if (holder.getParamMajorType(varArgIndex).getMode() == DataMode.REQUIRED
            && holder.getParamMajorType(varArgIndex).getMode() != argumentTypes.get(i).getMode()) {
          // prohibit using vararg functions for types with different nullability
          // if function accepts required arguments, but provided optional
          return -1;
        }
      }

      // increase cost for var arg functions to prioritize regular ones
      Integer additionalCost = ResolverTypePrecedence.precedenceMap.get(holder.getParamMajorType(varArgIndex).getMinorType());
      cost += additionalCost != null ? additionalCost : VARARG_COST;
      cost += holder.getParamMajorType(varArgIndex).getMode() == DataMode.REQUIRED ? 0 : 1;
    }

    if (secondaryCast) {
      // We have a secondary cast for one or more of the arguments, determine the cost associated
      int secondaryCastCost =  Integer.MAX_VALUE - 1;

      // Subtract maximum possible implicit costs from the secondary cast cost
      secondaryCastCost -= (nCasts * (ResolverTypePrecedence.MAX_IMPLICIT_CAST_COST + DATAMODE_CAST_COST));

      // Add cost of implicitly casting the rest of the arguments that didn't use secondary casting
      secondaryCastCost += cost;

      return secondaryCastCost;
    }

    return cost;
  }

  /*
   * Simple helper function to determine if input type is numeric
   */
  public static boolean isNumericType(MinorType inputType) {
    switch (inputType) {
      case TINYINT:
      case SMALLINT:
      case INT:
      case BIGINT:
      case UINT1:
      case UINT2:
      case UINT4:
      case UINT8:
      case DECIMAL9:
      case DECIMAL18:
      case DECIMAL28SPARSE:
      case DECIMAL38SPARSE:
      case VARDECIMAL:
      case FLOAT4:
      case FLOAT8:
        return true;
      default:
        return false;
    }
  }

}
