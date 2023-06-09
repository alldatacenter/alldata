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
import java.util.Map;
import java.util.Set;

import org.apache.drill.common.types.TypeProtos.MinorType;

public class ResolverTypePrecedence {


  public static final Map<MinorType, Integer> precedenceMap;
  public static final Map<MinorType, Set<MinorType>> secondaryImplicitCastRules;
  public static int MAX_IMPLICIT_CAST_COST;

  static {
    /* The precedenceMap is used to decide whether it's allowed to implicitly "promote"
     * one type to another type.
     *
     * The order that each type is inserted into HASHMAP decides its precedence.
     * First in ==> lowest precedence.
     * A type of lower precedence can be implicitly "promoted" to type of higher precedence.
     * For instance, NULL could be promoted to any other type;
     * tinyint could be promoted into int; but int could NOT be promoted into tinyint (due to possible precision loss).
     */
    int i = 0;
    precedenceMap = new HashMap<>();
    precedenceMap.put(MinorType.NULL, i += 2);       // NULL is legal to implicitly be promoted to any other type
    precedenceMap.put(MinorType.FIXEDBINARY, i += 2); // Fixed-length is promoted to var length
    precedenceMap.put(MinorType.VARBINARY, i += 2);
    precedenceMap.put(MinorType.FIXEDCHAR, i += 2);
    precedenceMap.put(MinorType.VARCHAR, i += 2);
    precedenceMap.put(MinorType.FIXED16CHAR, i += 2);
    precedenceMap.put(MinorType.VAR16CHAR, i += 2);
    precedenceMap.put(MinorType.BIT, i += 2);
    precedenceMap.put(MinorType.TINYINT, i += 2);   //type with few bytes is promoted to type with more bytes ==> no data loss.
    precedenceMap.put(MinorType.UINT1, i += 2);     //signed is legal to implicitly be promoted to unsigned.
    precedenceMap.put(MinorType.SMALLINT, i += 2);
    precedenceMap.put(MinorType.UINT2, i += 2);
    precedenceMap.put(MinorType.INT, i += 2);
    precedenceMap.put(MinorType.UINT4, i += 2);
    precedenceMap.put(MinorType.BIGINT, i += 2);
    precedenceMap.put(MinorType.UINT8, i += 2);
    precedenceMap.put(MinorType.MONEY, i += 2);
    precedenceMap.put(MinorType.FLOAT4, i += 2);
    precedenceMap.put(MinorType.DECIMAL9, i += 2);
    precedenceMap.put(MinorType.DECIMAL18, i += 2);
    precedenceMap.put(MinorType.DECIMAL28DENSE, i += 2);
    precedenceMap.put(MinorType.DECIMAL28SPARSE, i += 2);
    precedenceMap.put(MinorType.DECIMAL38DENSE, i += 2);
    precedenceMap.put(MinorType.DECIMAL38SPARSE, i += 2);
    precedenceMap.put(MinorType.VARDECIMAL, i += 2);
    precedenceMap.put(MinorType.FLOAT8, i += 2);
    precedenceMap.put(MinorType.DATE, i += 2);
    precedenceMap.put(MinorType.TIMESTAMP, i += 2);
    precedenceMap.put(MinorType.TIMETZ, i += 2);
    precedenceMap.put(MinorType.TIMESTAMPTZ, i += 2);
    precedenceMap.put(MinorType.TIME, i += 2);
    precedenceMap.put(MinorType.INTERVALDAY, i+= 2);
    precedenceMap.put(MinorType.INTERVALYEAR, i+= 2);
    precedenceMap.put(MinorType.INTERVAL, i+= 2);
    precedenceMap.put(MinorType.MAP, i += 2);
    precedenceMap.put(MinorType.DICT, i += 2);
    precedenceMap.put(MinorType.LIST, i += 2);
    precedenceMap.put(MinorType.UNION, i += 2);

    MAX_IMPLICIT_CAST_COST = i;

    /* Currently implicit cast follows the precedence rules.
     * It may be useful to perform an implicit cast in
     * the opposite direction as specified by the precedence rules.
     *
     * For example: As per the precedence rules we can implicitly cast
     * from VARCHAR ---> BIGINT , but based upon some functions (eg: substr, concat)
     * it may be useful to implicitly cast from BIGINT ---> VARCHAR.
     *
     * To allow for such cases we have a secondary set of rules which will allow the reverse
     * implicit casts. Currently we only allow the reverse implicit cast to VARCHAR so we don't
     * need any cost associated with it, if we add more of these that may collide we can add costs.
     */
    secondaryImplicitCastRules = new HashMap<>();
    HashSet<MinorType> rule = new HashSet<>();

    // Following cast functions should exist
    rule.add(MinorType.TINYINT);
    rule.add(MinorType.SMALLINT);
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.UINT1);
    rule.add(MinorType.UINT2);
    rule.add(MinorType.UINT4);
    rule.add(MinorType.UINT8);
    rule.add(MinorType.VARDECIMAL);
    rule.add(MinorType.MONEY);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.BIT);
    rule.add(MinorType.FIXEDCHAR);
    rule.add(MinorType.FIXED16CHAR);
    rule.add(MinorType.VARCHAR);
    rule.add(MinorType.DATE);
    rule.add(MinorType.TIME);
    rule.add(MinorType.TIMESTAMP);
    rule.add(MinorType.TIMESTAMPTZ);
    rule.add(MinorType.INTERVAL);
    rule.add(MinorType.INTERVALYEAR);
    rule.add(MinorType.INTERVALDAY);
    secondaryImplicitCastRules.put(MinorType.VARCHAR, rule);

    rule = new HashSet<>();

    // Be able to implicitly cast to VARBINARY
    rule.add(MinorType.INT);
    rule.add(MinorType.BIGINT);
    rule.add(MinorType.FLOAT4);
    rule.add(MinorType.FLOAT8);
    rule.add(MinorType.VARCHAR);
    secondaryImplicitCastRules.put(MinorType.VARBINARY, rule);

    rule = new HashSet<>();

    // Be able to implicitly cast to VARDECIMAL
    rule.add(MinorType.FLOAT8);
    secondaryImplicitCastRules.put(MinorType.VARDECIMAL, rule);

    rule = new HashSet<>();

    // Be able to implicitly cast to DECIMAL9
    rule.add(MinorType.VARDECIMAL);
    secondaryImplicitCastRules.put(MinorType.DECIMAL9, rule);

    rule = new HashSet<>();

    // Be able to implicitly cast to DECIMAL18
    rule.add(MinorType.VARDECIMAL);
    secondaryImplicitCastRules.put(MinorType.DECIMAL18, rule);

    rule = new HashSet<>();

    // Be able to implicitly cast to DECIMAL28SPARSE
    rule.add(MinorType.VARDECIMAL);
    secondaryImplicitCastRules.put(MinorType.DECIMAL28SPARSE, rule);

    rule = new HashSet<>();

    // Be able to implicitly cast to DECIMAL38SPARSE
    rule.add(MinorType.VARDECIMAL);
    secondaryImplicitCastRules.put(MinorType.DECIMAL38SPARSE, rule);
  }
}
