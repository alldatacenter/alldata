/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.ambari.server.api.predicate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;

import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.CategoryIsEmptyPredicate;
import org.apache.ambari.server.controller.predicate.EqualsPredicate;
import org.apache.ambari.server.controller.predicate.FilterPredicate;
import org.apache.ambari.server.controller.predicate.GreaterEqualsPredicate;
import org.apache.ambari.server.controller.predicate.LessEqualsPredicate;
import org.apache.ambari.server.controller.predicate.LessPredicate;
import org.apache.ambari.server.controller.predicate.NotPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.junit.Assert;
import org.junit.Test;

/**
 * QueryParser unit tests.
 */
public class QueryParserTest {

  @Test
  public void testParse_simple() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    //a=b
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "a"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "b"));

    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));

    assertEquals(new EqualsPredicate<>("a", "b"), p);
  }

  @Test
  public void testParse() throws InvalidQueryException {
    List<Token> listTokens = new ArrayList<>();
    // foo=bar&(a<1&(b<=2|c>3)&d>=100)|e!=5&!(f=6|g=7)
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "foo"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "bar"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "&"));
    listTokens.add(new Token(Token.TYPE.BRACKET_OPEN, "("));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "<"));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "a"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "1"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "&"));
    listTokens.add(new Token(Token.TYPE.BRACKET_OPEN, "("));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "<="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "b"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "2"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "|"));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, ">"));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "c"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "3"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "&"));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, ">="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "d"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "100"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "|"));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "!="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "e"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "5"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "&"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_UNARY_OPERATOR, "!"));
    listTokens.add(new Token(Token.TYPE.BRACKET_OPEN, "("));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "f"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "6"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "|"));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "g"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "7"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));

    EqualsPredicate<String> fooPred = new EqualsPredicate<>("foo", "bar");
    LessPredicate<String> aPred = new LessPredicate<>("a", "1");
    LessEqualsPredicate<String> bPred = new LessEqualsPredicate<>("b", "2");
    GreaterEqualsPredicate<String> cPred = new GreaterEqualsPredicate<>("c", "3");
    GreaterEqualsPredicate<String> dPred = new GreaterEqualsPredicate<>("d", "100");
    NotPredicate ePred = new NotPredicate(new EqualsPredicate<>("e", "5"));
    EqualsPredicate fPred = new EqualsPredicate<>("f", "6");
    EqualsPredicate gPRed = new EqualsPredicate<>("g", "7");
    OrPredicate bORcPred = new OrPredicate(bPred, cPred);
    AndPredicate aANDbORcPred = new AndPredicate(aPred, bORcPred);
    AndPredicate aANDbORcANDdPred = new AndPredicate(aANDbORcPred, dPred);
    AndPredicate fooANDaANDbORcANDdPred = new AndPredicate(fooPred, aANDbORcANDdPred);
    OrPredicate fORgPred = new OrPredicate(fPred, gPRed);
    NotPredicate NOTfORgPred = new NotPredicate(fORgPred);
    AndPredicate eANDNOTfORgPred = new AndPredicate(ePred, NOTfORgPred);
    OrPredicate rootPredicate = new OrPredicate(fooANDaANDbORcANDdPred, eANDNOTfORgPred);

    assertEquals(rootPredicate, p);
  }

  @Test
  public void testParse_NotOp__simple() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    //!a=b
    listTokens.add(new Token(Token.TYPE.LOGICAL_UNARY_OPERATOR, "!"));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "a"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "b"));

    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));

    assertEquals(new NotPredicate(new EqualsPredicate<>("a", "b")), p);
  }

  @Test
  public void testParse_NotOp() throws Exception {
    List<Token> listTokens = new ArrayList<>();
     //a=1&!b=2
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "a"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "1"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "&"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_UNARY_OPERATOR, "!"));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "b"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "2"));

    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));

    EqualsPredicate aPred = new EqualsPredicate<>("a", "1");
    EqualsPredicate bPred = new EqualsPredicate<>("b", "2");
    NotPredicate notPred = new NotPredicate(bPred);
    AndPredicate andPred = new AndPredicate(aPred, notPred);

    assertEquals(andPred, p);
  }

  @Test
  public void testParse_InOp__simple() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    // foo.in(one,two,3)
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".in("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "foo"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "one,two,3"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));

    EqualsPredicate ep1 = new EqualsPredicate<>("foo", "one");
    EqualsPredicate ep2 = new EqualsPredicate<>("foo", "two");
    EqualsPredicate ep3 = new EqualsPredicate<>("foo", "3");

    OrPredicate orPredicate = new OrPredicate(ep1, ep2, ep3);

    assertEquals(orPredicate, p);
  }

  @Test
  public void testParse_InOp__HostName() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    // foo.in(one,two,3)
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".in("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "HostRoles/host_name"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "Host1,HOST2,HoSt3"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));

    EqualsPredicate ep1 = new EqualsPredicate<>("HostRoles/host_name", "host1");
    EqualsPredicate ep2 = new EqualsPredicate<>("HostRoles/host_name", "host2");
    EqualsPredicate ep3 = new EqualsPredicate<>("HostRoles/host_name", "host3");

    OrPredicate orPredicate = new OrPredicate(ep1, ep2, ep3);

    assertEquals(orPredicate, p);
  }

  @Test
  public void testParse_InOp__HostName_Empty() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    // foo.in(one,two,3)
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".in("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "Hosts/host_name"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    try {
      Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));
      Assert.fail();
    } catch (InvalidQueryException e) {
      Assert.assertEquals(e.getMessage(), "IN operator is missing a required right operand for property Hosts/host_name");
    }
  }

  @Test
  public void testParse_EquOp_HostName() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    //a=1&!b=2
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "HostRoles/host_name"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "HOST1"));


    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));
    EqualsPredicate equalsPred = new EqualsPredicate<>("HostRoles/host_name", "host1");


    assertEquals(equalsPred, p);
  }

  @Test
  public void testParse_InOp__exception() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    // foo.in()
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".in("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "foo"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    try {
      parser.parse(listTokens.toArray(new Token[listTokens.size()]));
      fail("Expected InvalidQueryException due to missing right operand");
    } catch (InvalidQueryException e) {
      // expected
    }
  }

  @Test
  public void testParse_FilterOp() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".matches("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "foo"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, ".*"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));

    FilterPredicate fp = new FilterPredicate("foo", ".*");

    assertEquals(fp, p);
  }

  @Test
  public void testParse_FilterOp_exception() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".matches("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "foo"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    try {
      Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));
      fail("Filter operator is missing a required right operand.");
    } catch (InvalidQueryException e) {
      // expected
    }
  }

  @Test
  public void testParse_isEmptyOp__simple() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    // category1.isEmpty()
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".isEmpty("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "category1"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    Predicate p = parser.parse(listTokens.toArray(new Token[listTokens.size()]));

    assertEquals(new CategoryIsEmptyPredicate("category1"), p);
  }

  @Test
  public void testParse_isEmptyOp__exception() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    // category1.isEmpty()
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".isEmpty("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "category1"));
    // missing closing bracket

    QueryParser parser = new QueryParser();
    try {
      parser.parse(listTokens.toArray(new Token[listTokens.size()]));
      fail("Expected InvalidQueryException due to missing closing bracket");
    } catch (InvalidQueryException e) {
      // expected
    }
  }

  @Test
  public void testParse_isEmptyOp__exception2() throws Exception {
    List<Token> listTokens = new ArrayList<>();
    // category1.isEmpty()
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR_FUNC, ".isEmpty("));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "category1"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "one,two,3"));
    listTokens.add(new Token(Token.TYPE.BRACKET_CLOSE, ")"));

    QueryParser parser = new QueryParser();
    try {
      parser.parse(listTokens.toArray(new Token[listTokens.size()]));
      fail("Expected InvalidQueryException due to existence of right operand");
    } catch (InvalidQueryException e) {
      // expected
    }
  }

  @Test
  public void testParse_noTokens() throws InvalidQueryException {
    assertNull(new QueryParser().parse(new Token[0]));
  }

  @Test
  public void testParse_mismatchedBrackets() {
    List<Token> listTokens = new ArrayList<>();
    // a=1&(b<=2|c>3
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "a"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "1"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "&"));
    listTokens.add(new Token(Token.TYPE.BRACKET_OPEN, "("));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "<="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "b"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "2"));
    listTokens.add(new Token(Token.TYPE.LOGICAL_OPERATOR, "|"));
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, ">"));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "c"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "3"));

    try {
      new QueryParser().parse(listTokens.toArray(new Token[listTokens.size()]));
      fail("Expected InvalidQueryException due to missing closing bracket");
    } catch (InvalidQueryException e) {
      // expected
    }
  }

  @Test
  public void testParse_outOfOrderTokens() {
    List<Token> listTokens = new ArrayList<>();
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));
    listTokens.add(new Token(Token.TYPE.PROPERTY_OPERAND, "a"));
    listTokens.add(new Token(Token.TYPE.VALUE_OPERAND, "1"));
    // should be a logical operator
    listTokens.add(new Token(Token.TYPE.RELATIONAL_OPERATOR, "="));

    try {
      new QueryParser().parse(listTokens.toArray(new Token[listTokens.size()]));
      fail("Expected InvalidQueryException due to invalid last token");
    } catch (InvalidQueryException e) {
      // expected
    }
  }
}
