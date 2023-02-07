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
package org.apache.drill.common.expression.parser;

import org.apache.drill.common.exceptions.ExpressionParsingException;
import org.apache.drill.common.expression.ExpressionStringBuilder;
import org.apache.drill.common.expression.LogicalExpression;
import org.apache.drill.common.parser.LogicalExpressionParser;
import org.apache.drill.test.DrillTest;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TreeTest extends DrillTest {

  @Test
  void escapeStringLiteral() {
    String expr = "func(`identifier`, '\\\\d+', 0, 'fjds')";
    testExpressionParsing(expr, expr);
  }

  @Test
  void escapeQuotedIdentifier() {
    String expr = "`a\\\\b` + `c'd`";
    testExpressionParsing(expr, "add(`a\\\\b`, `c'd`)");
  }

  @Test
  void testIfWithCase() {
    testExpressionParsing("if ($F1) then case when (_MAP.R_NAME = 'AFRICA') then 2 else 4 end else if(4==3) then 1 else if(x==3) then 7 else (if(2==1) then 6 else 4 end) end",
      "( if (equal(`x`, 3)  ) then (7 )  else ( ( if (equal(2, 1)  ) then (6 )  else (4 )  end  )  )  end  )");
  }

  @Test
  void testAdd() {
    testExpressionParsing("2+2", "add(2, 2)");
  }

  @Test
  void testIf() {
    testExpressionParsing("if ('blue.red') then 'orange' else if (false) then 1 else 0 end",
      "( if (false ) then (1 )  else (0 )  end  )");
  }

  @Test
  void testQuotedIdentifier() {
    String expr = "`hello friend`.`goodbye`";
    testExpressionParsing(expr, expr);
  }

  @Test
  void testSpecialQuoted() {
    testExpressionParsing("`*0` + `*` ", "add(`*0`, `*`)");
  }

  @Test
  void testQuotedIdentifier2() {
    testExpressionParsing("`hello friend`.goodbye", "`hello friend`.`goodbye`");
  }

  @Test
  void testComplexIdentifier() {
    testExpressionParsing("goodbye[4].`hello`", "`goodbye`[4].`hello`");
  }

  @Test // DRILL-2606
  void testCastToBooleanExpr() {
    String expr = "cast( (cast( (`bool_col` ) as VARCHAR(100) ) ) as BIT )";
    testExpressionParsing(expr, expr);
  }

  @Test
  void testComments() {
    testExpressionParsing("cast /* block comment */ ( // single comment\n" +
      "1 as int)", "cast( (1 ) as INT )");
  }

  @Test
  void testParsingException() {

    ExpressionParsingException exception = assertThrows(ExpressionParsingException.class, () ->
      testExpressionParsing("cast(1 as i)", "")
    );
    assertThat(exception.getMessage(), containsString("mismatched input 'i' expecting"));
  }

  @Test
  void testFunctionCallWithoutParams() {
    String expr = "now()";
    testExpressionParsing(expr, expr);
  }

  /**
   * Attempt to parse an expression.  Once parsed, convert it to a string and then parse it again to make sure serialization works.
   */
  private void testExpressionParsing(String expr, String expected) {
    LogicalExpression e1 = LogicalExpressionParser.parse(expr);
    String newStringExpr = serializeExpression(e1);
    assertEquals(expected, newStringExpr.trim());
    LogicalExpressionParser.parse(newStringExpr);
  }

  private String serializeExpression(LogicalExpression expr){
    ExpressionStringBuilder b = new ExpressionStringBuilder();
    StringBuilder sb = new StringBuilder();
    expr.accept(b, sb);
    return sb.toString();
  }

}
