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
package org.apache.drill.common;

public interface FunctionNames {

  String NEGATE = "negative";
  String ADD = "add";
  String SUBTRACT = "subtract";
  String MULTIPLY = "multiply";
  String DIVIDE = "divide";
  String MODULO = "modulo";

  String NOT = "not";
  String OR = "booleanOr";
  String AND = "booleanAnd";
  String XOR = "xor";

  String COMPARE_TO_NULLS_HIGH = "compare_to_nulls_high";
  String COMPARE_TO_NULLS_LOW = "compare_to_nulls_low";

  String EQ = "equal";
  String NE = "not_equal";
  String GT = "greater_than";
  String GE = "greater_than_or_equal_to";
  String LT = "less_than";
  String LE = "less_than_or_equal_to";

  String IS_NULL = "isnull";
  String IS_NOT_NULL = "isnotnull";
  String IS_TRUE = "istrue";
  String IS_NOT_TRUE = "isnottrue";
  String IS_FALSE = "isfalse";
  String IS_NOT_FALSE = "isnotfalse";

  String CONCAT = "concatOperator";
  String SIMILAR_TO = "similar_to";
  String LIKE = "like";
}
