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
package org.apache.drill.exec.expr.fn.impl;

import static org.junit.Assert.assertTrue;

import org.apache.commons.lang3.RandomStringUtils;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.BaseTestQuery;
import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.exec.util.Text;
import org.junit.Test;

import org.apache.drill.shaded.guava.com.google.common.collect.ImmutableList;
import org.junit.experimental.categories.Category;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

@Category({SqlFunctionTest.class, UnlikelyTest.class})
public class TestStringFunctions extends BaseTestQuery {

  @Test
  public void testStrPosMultiByte() throws Exception {
    testBuilder()
        .sqlQuery("select `position`('a', 'abc') res1 from (values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues(1L)
        .go();

    testBuilder()
        .sqlQuery("select `position`('\\u11E9', '\\u11E9\\u0031') res1 from (values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues(1L)
        .go();
  }

  @Test
  public void testSplitPart() throws Exception {
    testBuilder()
        .sqlQuery("select split_part(a, '~@~', 1) res1 from (values('abc~@~def~@~ghi'), ('qwe~@~rty~@~uio')) as t(a)")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("abc")
        .baselineValues("qwe")
        .go();

    testBuilder()
        .sqlQuery("select split_part(a, '~@~', 2) res1 from (values('abc~@~def~@~ghi'), ('qwe~@~rty~@~uio')) as t(a)")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("def")
        .baselineValues("rty")
        .go();

    testBuilder()
      .sqlQuery("select split_part(a, '~@~', -2) res1 from (values('abc~@~def~@~ghi'), ('qwe~@~rty~@~uio')) as t(a)")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("def")
      .baselineValues("rty")
      .go();

    // with a multi-byte splitter
    testBuilder()
        .sqlQuery("select split_part('abc\\u1111drill\\u1111ghi', '\\u1111', 2) res1 from (values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("drill")
        .go();

    testBuilder()
      .sqlQuery("select split_part('abc\\u1111drill\\u1111ghi', '\\u1111', -2) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("drill")
      .go();

    // going beyond the last available index, returns empty string
    testBuilder()
        .sqlQuery("select split_part('a,b,c', ',', 4) res1 from (values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("")
        .go();

    testBuilder()
      .sqlQuery("select split_part('a,b,c', ',', -4) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("")
      .go();

    // if the delimiter does not appear in the string, 1 returns the whole string
    testBuilder()
        .sqlQuery("select split_part('a,b,c', ' ', 1) res1 from (values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("a,b,c")
        .go();

    testBuilder()
      .sqlQuery("select split_part('a,b,c', ' ', -1) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("a,b,c")
      .go();
  }

  @Test
  public void testSplitPartStartEnd() throws Exception {
    testBuilder()
      .sqlQuery("select split_part(a, '~@~', 1, 2) res1 from (" +
        "values('abc~@~def~@~ghi'), ('qwe~@~rty~@~uio')) as t(a)")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("abc~@~def")
      .baselineValues("qwe~@~rty")
      .go();

    testBuilder()
      .sqlQuery("select split_part(a, '~@~', 2, 3) res1 from (" +
        "values('abc~@~def~@~ghi'), ('qwe~@~rty~@~uio')) as t(a)")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("def~@~ghi")
      .baselineValues("rty~@~uio")
      .go();

    testBuilder()
      .sqlQuery("select split_part(a, '~@~', -2, -1) res1 from (" +
        "values('abc~@~def~@~ghi'), ('qwe~@~rty~@~uio')) as t(a)")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("def~@~ghi")
      .baselineValues("rty~@~uio")
      .go();

    // with a multi-byte splitter
    testBuilder()
      .sqlQuery("select split_part('abc\\u1111drill\\u1111ghi', '\\u1111', 2, 2) " +
        "res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("drill")
      .go();

    testBuilder()
      .sqlQuery("select split_part('abc\\u1111drill\\u1111ghi', '\\u1111', -2, -2) " +
        "res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("drill")
      .go();

    // start index going beyond the last available index, returns empty string
    testBuilder()
      .sqlQuery("select split_part('a,b,c', ',', 4, 5) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("")
      .go();

    testBuilder()
      .sqlQuery("select split_part('a,b,c', ',', -5, -4) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("")
      .go();

    // end index going beyond the last available index, returns remaining string
    testBuilder()
      .sqlQuery("select split_part('a,b,c', ',', 1, 10) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("a,b,c")
      .go();

    testBuilder()
      .sqlQuery("select split_part('a,b,c', ',', -10, -1) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("a,b,c")
      .go();

    // if the delimiter does not appear in the string, 1 returns the whole string
    testBuilder()
      .sqlQuery("select split_part('a,b,c', ' ', 1, 2) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("a,b,c")
      .go();

    testBuilder()
      .sqlQuery("select split_part('a,b,c', ' ', -2, -1) res1 from (values(1))")
      .ordered()
      .baselineColumns("res1")
      .baselineValues("a,b,c")
      .go();
  }

  @Test
  public void testInvalidSplitPartParameters() {
    boolean expectedErrorEncountered;
    try {
      testBuilder()
        .sqlQuery("select split_part('abc~@~def~@~ghi', '~@~', 0) res1 from " +
          "(values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("abc")
        .go();
      expectedErrorEncountered = false;
    } catch (Exception ex) {
      assertTrue(ex.getMessage(),
        ex.getMessage().contains("Index in split_part can not be zero"));
      expectedErrorEncountered = true;
    }
    if (!expectedErrorEncountered) {
      throw new RuntimeException("Missing expected error on invalid index for " +
        "split_part function");
    }

    try {
      testBuilder()
        .sqlQuery("select split_part('abc~@~def~@~ghi', '~@~', 2, 1) res1 from " +
          "(values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("abc")
        .go();
      expectedErrorEncountered = false;
    } catch (Exception ex) {
      assertTrue(ex.getMessage(),
        ex.getMessage().contains("End index in split_part must be greater or equal to start index"));
      expectedErrorEncountered = true;
    }
    if (!expectedErrorEncountered) {
      throw new RuntimeException("Missing expected error on invalid index for " +
        "split_part function");
    }

    try {
      testBuilder()
        .sqlQuery("select split_part('abc~@~def~@~ghi', '~@~', -1, -2) res1 from " +
          "(values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("abc")
        .go();
      expectedErrorEncountered = false;
    } catch (Exception ex) {
      assertTrue(ex.getMessage(),
        ex.getMessage().contains("End index in split_part must be greater or equal to start index"));
      expectedErrorEncountered = true;
    }
    if (!expectedErrorEncountered) {
      throw new RuntimeException("Missing expected error on invalid index for " +
        "split_part function");
    }

    try {
      testBuilder()
        .sqlQuery("select split_part('abc~@~def~@~ghi', '~@~', -1, 2) res1 from " +
          "(values(1))")
        .ordered()
        .baselineColumns("res1")
        .baselineValues("abc")
        .go();
      expectedErrorEncountered = false;
    } catch (Exception ex) {
      assertTrue(ex.getMessage(),
        ex.getMessage().contains("End index in split_part must has the same sign as the start index"));
      expectedErrorEncountered = true;
    }
    if (!expectedErrorEncountered) {
      throw new RuntimeException("Missing expected error on invalid index for " +
        "split_part function");
    }
  }

  @Test
  public void testRegexpMatches() throws Exception {
    testBuilder()
        .sqlQuery("select regexp_matches(a, '^a.*') res1, regexp_matches(b, '^a.*') res2 " +
                  "from (values('abc', 'bcd'), ('bcd', 'abc')) as t(a,b)")
        .unOrdered()
        .baselineColumns("res1", "res2")
        .baselineValues(true, false)
        .baselineValues(false, true)
        .build()
        .run();
  }

  @Test
  public void testRegexpMatchesNonAscii() throws Exception {
    testBuilder()
        .sqlQuery("select regexp_matches(a, 'München') res1, regexp_matches(b, 'AMünchenA') res2 " +
            "from (values('München', 'MünchenA'), ('MünchenA', 'AMünchenA')) as t(a,b)")
        .unOrdered()
        .baselineColumns("res1", "res2")
        .baselineValues(true, false)
        .baselineValues(false, true)
        .build()
        .run();
  }

  @Test
  public void testRegexpReplace() throws Exception {
    testBuilder()
        .sqlQuery("select regexp_replace(a, 'a|c', 'x') res1, regexp_replace(b, 'd', 'zzz') res2 " +
                  "from (values('abc', 'bcd'), ('bcd', 'abc')) as t(a,b)")
        .unOrdered()
        .baselineColumns("res1", "res2")
        .baselineValues("xbx", "bczzz")
        .baselineValues("bxd", "abc")
        .build()
        .run();
  }

  @Test
  public void testReplaceOutBuffer() throws Exception {
    String originValue = RandomStringUtils.randomAlphabetic(8192).toLowerCase() + "12345";
    String expectValue = originValue.replace("12345", "67890");
    String sql = "select replace(c1, '12345', '67890') as col from (values('" + originValue + "')) as t(c1)";
    testBuilder()
      .sqlQuery(sql)
      .ordered()
      .baselineColumns("col")
      .baselineValues(expectValue)
      .go();
  }

  @Test
  public void testLikeStartsWith() throws Exception {

    // all ASCII.
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), ('ABCD'),('ABCDE')," +
            "('AABCD'),('ABABCD'),('ABC$XYZ'), (''),('abcd')," +
            "('x'), ('xyz'), ('%')) tbl(id) " +
            "where id like 'ABC%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC")
        .baselineValues("ABCD")
        .baselineValues("ABCDE")
        .baselineValues("ABC$XYZ")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD')," +
            "('ABCD'),('ABCDE'),('AABCD'),('ABAB CD'),('ABC$XYZ')," +
            "(''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like 'AB%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("AB")
        .baselineValues("ABC")
        .baselineValues("ABD")
        .baselineValues("ABCD")
        .baselineValues("ABCDE")
        .baselineValues("ABAB CD")
        .baselineValues("ABC$XYZ")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'), ('ABC'), ('ABD'), ('ABCD')," +
            "('ABCDE'),('AABCD'),('ABAB CD'),('ABC$XYZ'), ('')," +
            "('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like 'A%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A")
        .baselineValues("AB")
        .baselineValues("ABC")
        .baselineValues("ABD")
        .baselineValues("ABCD")
        .baselineValues("ABCDE")
        .baselineValues("AABCD")
        .baselineValues("ABAB CD")
        .baselineValues("ABC$XYZ")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE')," +
            "('AABCD'), ('ABABCD'),('ABC$XYZ'), (''),('abcd')," +
            "('x'), ('xyz'), ('%')) tbl(id)" +
            " where id like 'z%'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // patternLength > txtLength
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD')," +
            "('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like 'ABCDEXYZRST%'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // non ASCII
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~')," +
            " ('xyz'), ('%')) tbl(id)" +
            " where id like '¤%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), " +
            "('xyz'), ('%')) tbl(id)" +
            " where id like 'ABC¤%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), ('xyz'), ('%')) tbl(id)" +
            " where id like 'A%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC")
        .baselineValues("ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), ('xyz'), ('%')) tbl(id) " +
            "where id like 'Z%'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();
  }

  @Test
  public void testLikeEndsWith() throws Exception {

    // all ASCII. End with multiple characters
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), ('ABCD'),('ABCDE')," +
            "('AABCD'),('ABABCD'),('ABC$XYZ'), (''),('abcd'), " +
            "('x'), ('xyz'), ('%')) tbl(id) " +
            "where id like '%BCD'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABCD")
        .baselineValues("AABCD")
        .baselineValues("ABABCD")
        .build()
        .run();

    // all ASCII. End with single character.
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), ('ABCD'),('ABCDE')," +
            "('AABCD'),('ABABCD'),('ABC$XYZ'), (''),('abcd'), " +
            "('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%D'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABD")
        .baselineValues("ABCD")
        .baselineValues("AABCD")
        .baselineValues("ABABCD")
        .build()
        .run();

    // all ASCII. End with nothing. Should match all.
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), ('ABCD'),('ABCDE')," +
            "('AABCD'),('ABABCD'),('ABC$XYZ'), (''),('abcd'), " +
            "('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A")
        .baselineValues("AB")
        .baselineValues("ABC")
        .baselineValues("ABD")
        .baselineValues("ABCD")
        .baselineValues("ABCDE")
        .baselineValues("AABCD")
        .baselineValues("ABABCD")
        .baselineValues("ABC$XYZ")
        .baselineValues("")
        .baselineValues("abcd")
        .baselineValues("x")
        .baselineValues("xyz")
        .baselineValues("%")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD')," +
            "('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%F'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // patternLength > txtLength
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD'),('ABABCD')," +
            "('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id) " +
            "where id like '%ABCDEXYZRST'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // patternLength == txtLength
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD'),('ABABCD')," +
            "('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id) " +
            "where id like '%ABC'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC")
        .build()
        .run();

    // non ASCII. End with single character
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), ('')," +
            "('¶TÆU2~~'), ('xyz'), ('%')) tbl(id) " +
            "where id like '%~~'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .baselineValues("¶TÆU2~~")
        .build()
        .run();

    // non ASCII. End with multiple characters
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), " +
            "(''), ('¶TÆU2~~'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%¶TÆU2~~'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .baselineValues("¶TÆU2~~")
        .build()
        .run();

    // non ASCII, no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), ('')," +
            "('xyz'), ('%')) tbl(id)" +
            "where id like '%E'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();
  }

  @Test
  public void testLikeContains() throws Exception {

    // all ASCII. match at the beginning, middle and end.
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), ('ABCD'),('DEABC')," +
            "('AABCD'), ('ABABCDEF'),('AABC$XYZ'), (''),('abcd'), ('x'), " +
            "('xyz'), ('%')) tbl(id) " +
            "where id like '%ABC%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC")
        .baselineValues("ABCD")
        .baselineValues("DEABC")
        .baselineValues("AABCD")
        .baselineValues("ABABCDEF")
        .baselineValues("AABC$XYZ")
        .build()
        .run();

    // all ASCII. match at the beginning, middle and end, single character.
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), ('ABCD'),('ABCDE')," +
            "('AABCD'),('ABABCD'),('CAB$XYZ'), (''),('abcd'), ('x'), " +
            "('xyz'), ('%')) tbl(id)" +
            "where id like '%C%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC")
        .baselineValues("ABCD")
        .baselineValues("ABCDE")
        .baselineValues("AABCD")
        .baselineValues("ABABCD")
        .baselineValues("CAB$XYZ")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), ('ABCD'),('ABCDE')," +
            "('AABCD'),('ABABCD'),('CAB$XYZ'), (''),('abcd'), ('x')," +
            "('xyz'), ('%')) tbl(id)" +
            "where id like '%FGH%'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // patternLength > txtLength
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD'),('ABABCD')," +
            "('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%ABCDEXYZRST%'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // all match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD'),('ABABCD')," +
            "('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A")
        .baselineValues("AB")
        .baselineValues("ABC")
        .baselineValues("ABCD")
        .baselineValues("ABCDE")
        .baselineValues("AABCD")
        .baselineValues("ABABCD")
        .baselineValues("ABC$XYZ")
        .baselineValues("")
        .baselineValues("abcd")
        .baselineValues("x")
        .baselineValues("xyz")
        .baselineValues("%")
        .build()
        .run();

    // non ASCII
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), " +
            "(''), ('¶TÆU2~~'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%ÆU2%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .baselineValues("¶TÆU2~~")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), " +
            "(''), ('¶TÆU2~~'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%EÀsÆW%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), ('')," +
            "('xyz'), ('%')) tbl(id) where id like '%¶T¶T%'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();
  }

  @Test
  public void testLikeConstant() throws Exception {

    // all ASCII
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), " +
            "('ABCD'),('ABCDE'),('AABCD'),('ABABCD'),('ABC$XYZ'), ('')," +
            "('abcd'), ('x'), ('xyz'), ('%')) tbl(id) " +
            "where id like 'ABC'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC")
        .build()
        .run();


    // Multiple same values
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABC')," +
            "('ABD'), ('ABCD'),('ABCDE'),('AABCD'),('ABABCD'),('ABC$XYZ')," +
            "(''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like 'ABC'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC")
        .baselineValues("ABC")
        .build()
        .run();

    // match empty string
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'), ('ABD'), ('ABCD'),('ABCDE')," +
            "('AABCD'),('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x')," +
            " ('xyz'), ('%')) tbl(id)" +
            "where id like ''")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD')," +
            "('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz')," +
            "('%')) tbl(id) where id like 'EFGH'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // patternLength > txtLength
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD'),('ABABCD')," +
            "('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            " where id like 'ABCDEXYZRST'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();


    // non ASCII
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), (''), " +
            "('¶TÆU2~~'), ('xyz'), ('%')) tbl(id)" +
            " where id like '¶TÆU2~~'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("¶TÆU2~~")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), ('')," +
            "('¶TÆU2~~'), ('xyz'), ('%')) tbl(id)" +
            "where id like 'ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), (''), " +
            "('xyz'), ('%')) tbl(id) where id like '¶T¶T'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();
  }

  @Test
  public void testLikeWithEscapeStartsWith() throws Exception {

    // all ASCII
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC%'), ('ABD'), ('ABCD'),('ABCDE')," +
            "('AABCD'),('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x')," +
            "('xyz'), ('%')) tbl(id) " +
            "where id like 'ABC#%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC%")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('A%B'),('A%B%C%'), ('ABD'), ('ABCD')," +
            "('ABCDE'),('AABCD'),('A%BABCD'),('ABC$XYZ'), ('')," +
            "('abcd'), ('x'), ('xyz'), ('%')) tbl (id)" +
            "where id like 'A#%B%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A%B")
        .baselineValues("A%B%C%")
        .baselineValues("A%BABCD")
        .build()
        .run();

    // Multiple escape characters
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABD'), ('A_BC%D_')," +
            "('ABCDE'), ('A_BC%D_XYZ'),('ABABCD'),('A_BC%D_$%XYZ')," +
            " (''),('abcd'), ('x'), ('%')) tbl(id)" +
            "where id like 'A#_BC#%D#_%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A_BC%D_")
        .baselineValues("A_BC%D_XYZ")
        .baselineValues("A_BC%D_$%XYZ")
        .build()
        .run();

    // Escape character followed by escape character
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABD'), ('ABC%D_')," +
            "('A#BC%D_E'),('A_BC%D_XYZ'),('ABABCD'),('A#BC%D_$%XYZ')," +
            " (''),('abcd'), ('x'), ('%')) tbl(id)" +
            "where id like 'A##BC#%D#_%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A#BC%D_E")
        .baselineValues("A#BC%D_$%XYZ")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD')," +
            "('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            " where id like 'z#%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // patternLength > txtLength
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB'),('ABC'),('ABCD'),('ABCDE'),('AABCD')," +
            "('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            " where id like 'ABCDEXYZRST#_%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    // non ASCII
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤E_ÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~')," +
            " ('xyz'), ('%')) tbl(id)" +
            " where id like '¤E#_%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("¤E_ÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀ%sÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), " +
            "('xyz'), ('%')) tbl(id)" +
            " where id like 'ABC¤EÀ#%sÆW%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC¤EÀ%sÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('AB%C'), " +
            "('AB%C¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~'), ('xyz'), ('%')) tbl(id)" +
            " where id like 'AB#%C%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("AB%C")
        .baselineValues("AB%C¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~')," +
            " ('xyz'), ('%')) tbl(id)" +
            "where id like 'Z$%%' escape '$'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();
  }

  @Test
  public void testLikeWithEscapeEndsWith() throws Exception {

    // all ASCII
    testBuilder().sqlQuery(" SELECT  id FROM (" +
        "VALUES('A'),('AB'),('ABC'),('AB%C'),('ABCDE'),('AABCD')," +
        "('ABAB%C'),('ABC$XYZAB%C'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id) " +
        "where id like '%AB$%C' escape '$'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("AB%C")
        .baselineValues("ABAB%C")
        .baselineValues("ABC$XYZAB%C")
        .build()
        .run();


    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB_'),('AB%C%AB_'), ('ABD'), ('ABCD')," +
            "('ABCDE'), ('AABCD'),('AB%ABCD'),('ABC$XYZAB_'), ('')," +
            "('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%AB#_' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("AB_")
        .baselineValues("AB%C%AB_")
        .baselineValues("ABC$XYZAB_")
        .build()
        .run();


    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABD'), ('A_BCD'),('ABCDEA_')," +
            "('A_ABCD'),('ABABCDA_'),('A_BC$XYZA_'), (''),('abcd')," +
            " ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%A#_' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues(("A_"))
        .baselineValues("ABCDEA_")
        .baselineValues("ABABCDA_")
        .baselineValues("A_BC$XYZA_")
        .build()
        .run();

    // Multiple escape characters
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABD'), ('A_BC%D_'),('ABCDE')," +
            "('XYZA_BC%D_'),('ABABCD'),('$%XYZA_BC%D_'), ('')," +
            "('abcd'), ('x'), ('%')) tbl(id)" +
            " where id like '%A#_BC#%D#_' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A_BC%D_")
        .baselineValues("XYZA_BC%D_")
        .baselineValues("$%XYZA_BC%D_")
        .build()
        .run();


    // Escape character followed by escape character
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABD'), ('A#BC%D_'),('A#BC%D_E')," +
            "('A_BC%D_XYZ'),('ABABCD'),('$%XYZA#BC%D_'), ('')," +
            "('abcd'), ('x'), ('%')) tbl(id)" +
            " where id like '%A##BC#%D#_' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A#BC%D_")
        .baselineValues("$%XYZA#BC%D_")
        .build()
        .run();

    // non ASCII
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤E_ÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2_~~')," +
            " ('xyz'), ('%')) tbl(id)" +
            " where id like '%2#_~~' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("¤E_ÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2_~~")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('ABC¤EÀ%sÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~ABC¤EÀ%sÆW'), " +
            "('xyz'), ('%')) tbl(id)" +
            " where id like '%ABC¤EÀ#%sÆW' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC¤EÀ%sÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~ABC¤EÀ%sÆW")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('AB%C'), " +
            "('AB%C¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~AB%C'), ('xyz'), ('%')) tbl(id)" +
            " where id like '%AB#%C' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("AB%C")
        .baselineValues("AB%C¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~AB%C")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('ABC'), ('¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~')," +
            " ('xyz'), ('%')) tbl(id)" +
            "where id like '%$%' escape '$'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("%")
        .build()
        .run();

  }

  @Test
  public void testLikeWithEscapeContains() throws Exception {

    // test EndsWith
    testBuilder().sqlQuery(" SELECT  id FROM (" +
        "VALUES('A'),('AB'),('ABC'),('AB%C'),('ABCDE'),('AB%AB%CDED')," +
        "('ABAB%CDE'),('ABC$XYZAB%C'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
        "where id like '%AB$%C%' escape '$'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABAB%CDE")
        .baselineValues("AB%AB%CDED")
        .baselineValues("AB%C")
        .baselineValues("ABC$XYZAB%C")
        .build()
        .run();


    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB_'),('%AB%C%AB_'), ('%AB%D'), ('ABCD')," +
            "('AB%AC%AB%DE'), ('AABCD'),('AB%AB%CD'),('ABC$XYZAB_')," +
            "(''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%#%AB#%%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("%AB%C%AB_")
        .baselineValues("%AB%D")
        .baselineValues("AB%AB%CD")
        .baselineValues("AB%AC%AB%DE")
        .build()
        .run();

    // no match
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A'),('AB_'),('%AB%C%AB_'), ('%AB%D'), ('ABCD')," +
            "('AB%AC%AB%DE'), ('AABCD'),('AB%AB%CD'),('ABC$XYZAB_')," +
            "(''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%#%A#_B#%%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABD'), ('A_BCD'),('ABA_CDEA_')," +
            "('A_ABCD'),('ABABCDA_'),('A_BC$XYZA_'), ('')," +
            "('abcd'), ('x'), ('xyz'), ('%')) tbl(id)" +
            "where id like '%A#_%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues(("A_"))
        .baselineValues("A_BCD")
        .baselineValues("ABA_CDEA_")
        .baselineValues("A_ABCD")
        .baselineValues("A_BC$XYZA_")
        .baselineValues("ABABCDA_")
        .build()
        .run();


    // Multiple escape characters
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABD'), ('A_BC%D_'),('ABCDE')," +
            "('XYZA_BC%D_'),('ABABCD'),('$%XYZA_BC%D_'), ('')," +
            "('abcd'), ('x'), ('%')) tbl" +
            "(id) where id like '%A#_BC#%D#_' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A_BC%D_")
        .baselineValues("XYZA_BC%D_")
        .baselineValues("$%XYZA_BC%D_")
        .build()
        .run();


    // Escape character followed by escape character
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABDA#BC%D_'), ('A#BC%D_')," +
            "('A#BC%BC%D_E'),('A_BC%D_XYZ'),('ABABCD'),('$%XYZA#BC%D_')," +
            " (''),('abcd'), ('x'), ('%')) tbl(id)" +
            " where id like '%A##BC#%D#_%' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A#BC%D_")
        .baselineValues("$%XYZA#BC%D_")
        .baselineValues("ABDA#BC%D_")
        .build()
        .run();

  }

  @Test
  public void testLikeWithEscapeConstant() throws Exception {

    // test startsWith
    testBuilder().sqlQuery(" SELECT  id FROM (" +
        "VALUES('A'),('AB'),('ABC%'),('ABCD'),('ABCDE'),('AABCD')," +
        "('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id) " +
        "where id like 'ABC%%' escape '%' ")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("ABC%")
        .build()
        .run();

    testBuilder().sqlQuery(" SELECT  id FROM (" +
        "VALUES('A'),('AB'),('%ABC'),('ABCD'),('ABCDE'),('AABCD')," +
        "('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id) " +
        "where id like '%%ABC' escape '%' ")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("%ABC")
        .build()
        .run();

    testBuilder().sqlQuery(" SELECT  id FROM (" +
        "VALUES('A'),('AB'),('AB%C'),('ABCD'),('ABCDE'),('AABCD'),('ABABCD')," +
        "('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id) " +
        "where id like 'AB%%C' escape '%' ")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("AB%C")
        .build()
        .run();

    // Multiple escape characters
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABD'), ('%_BC%D_'),('ABCDE')," +
            "('XYZA_BC%D_'),('ABABCD'),('$%XYZA_BC%D_'), (''),('abcd'), ('x'), ('%')) tbl(id)" +
            " where id like '%%_BC%%D%_' escape '%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("%_BC%D_")
        .build()
        .run();

    // Escape character followed by escape character
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('A_'),('AB'),('ABC'), ('ABDA#BC%D_'), ('A#BC%D_'),('A#BA#BC%D_E')," +
            "('A_BC%D_XYZ'),('ABABCD'),('$%XYZA#BC%D_'), (''),('abcd'), ('x'), ('%')) tbl(id)" +
            " where id like 'A##BC#%D#_' escape '#'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("A#BC%D_")
        .build()
        .run();

    // no match
    testBuilder().sqlQuery(" SELECT  id FROM (" +
        "VALUES('A'),('AB'),('ABC%'),('ABCD'),('ABCDE'),('AABCD')," +
        "('ABABCD'),('ABC$XYZ'), (''),('abcd'), ('x'), ('xyz'), ('%')) tbl(id) " +
        "where id like '%_ABC%%' escape '%' ")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

  }

  @Test
  public void testLikeRandom() throws Exception {

    // test Random queries with like
    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('aeiou'),('abcdef'),('afdrgt'),('abcdt'),('aaaa'),('a'),('aeiou'),(''),('a aa')) tbl(id)" +
            "where id not like 'a %'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("aeiou")
        .baselineValues("abcdef")
        .baselineValues("afdrgt")
        .baselineValues("abcdt")
        .baselineValues("aaaa")
        .baselineValues("a")
        .baselineValues("aeiou")
        .baselineValues("")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('aeiou'),('abcdefizu'),('afdrgt'),('abcdt'),('aaaa'),('a'),('aeiou'),(''),('a aa')) tbl(id)" +
            "where id like 'a%i_u'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("aeiou")
        .baselineValues("aeiou")
        .baselineValues("abcdefizu")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('xyzaeioughbcd'),('abcdefizu'),('afdrgt'),('abcdt'),('aaaa'),('a'),('aeiou'),(''),('a aa')) tbl(id)" +
            "where id like '%a_i_u%bcd%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("xyzaeioughbcd")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like 'ab'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like '%ab'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like 'ab%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("abc")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like '%ab%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("abc")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like 'abc'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("abc")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like 'abc%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("abc")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like '%abc'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("abc")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like '%abc%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("abc")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like 'abcd'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like 'abcd%'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like '%abcd'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like '%abcd%'")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like ''")
        .unOrdered()
        .baselineColumns("id")
        .expectsEmptyResultSet()
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like '%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("abc")
        .build()
        .run();

    testBuilder()
        .sqlQuery(" SELECT  id FROM (" +
            "VALUES('abc')) tbl(id)" +
            "where id like '%%'")
        .unOrdered()
        .baselineColumns("id")
        .baselineValues("abc")
        .build()
        .run();
  }

  @Test
  public void testILike() throws Exception {
    testBuilder()
        .sqlQuery("select n_name from cp.`tpch/nation.parquet` where ilike(n_name, '%united%') = true")
        .unOrdered()
        .baselineColumns("n_name")
        .baselineValues("UNITED STATES")
        .baselineValues("UNITED KINGDOM")
        .build()
        .run();
  }

  @Test
  public void testILikeEscape() throws Exception {
    testBuilder()
        .sqlQuery("select a from (select concat(r_name , '_region') a from cp.`tpch/region.parquet`) where ilike(a, 'asia#_region', '#') = true")
        .unOrdered()
        .baselineColumns("a")
        .baselineValues("ASIA_region")
        .build()
        .run();
  }

  @Test
  public void testSubstr() throws Exception {
    testBuilder()
        .sqlQuery("select substr(n_name, 'UN.TE.') a from cp.`tpch/nation.parquet` where ilike(n_name, 'united%') = true")
        .unOrdered()
        .baselineColumns("a")
        .baselineValues("UNITED")
        .baselineValues("UNITED")
        .build()
        .run();
  }

  @Test
  public void testLpadTwoArgConvergeToLpad() throws Exception {
    final String query_1 = "SELECT lpad(r_name, 25) \n" +
        "FROM cp.`tpch/region.parquet`";


    final String query_2 = "SELECT lpad(r_name, 25, ' ') \n" +
        "FROM cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query_1)
        .unOrdered()
        .sqlBaselineQuery(query_2)
        .build()
        .run();
  }

  @Test
  public void testRpadTwoArgConvergeToRpad() throws Exception {
    final String query_1 = "SELECT rpad(r_name, 25) \n" +
        "FROM cp.`tpch/region.parquet`";


    final String query_2 = "SELECT rpad(r_name, 25, ' ') \n" +
        "FROM cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query_1)
        .unOrdered()
        .sqlBaselineQuery(query_2)
        .build()
        .run();
  }

  @Test
  public void testLtrimOneArgConvergeToLtrim() throws Exception {
    final String query_1 = "SELECT ltrim(concat(' ', r_name, ' ')) \n" +
        "FROM cp.`tpch/region.parquet`";


    final String query_2 = "SELECT ltrim(concat(' ', r_name, ' '), ' ') \n" +
        "FROM cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query_1)
        .unOrdered()
        .sqlBaselineQuery(query_2)
        .build()
        .run();
  }

  @Test
  public void testRtrimOneArgConvergeToRtrim() throws Exception {
    final String query_1 = "SELECT rtrim(concat(' ', r_name, ' ')) \n" +
        "FROM cp.`tpch/region.parquet`";


    final String query_2 = "SELECT rtrim(concat(' ', r_name, ' '), ' ') \n" +
        "FROM cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query_1)
        .unOrdered()
        .sqlBaselineQuery(query_2)
        .build()
        .run();
  }

  @Test
  public void testBtrimOneArgConvergeToBtrim() throws Exception {
    final String query_1 = "SELECT btrim(concat(' ', r_name, ' ')) \n" +
        "FROM cp.`tpch/region.parquet`";


    final String query_2 = "SELECT btrim(concat(' ', r_name, ' '), ' ') \n" +
        "FROM cp.`tpch/region.parquet`";

    testBuilder()
        .sqlQuery(query_1)
        .unOrdered()
        .sqlBaselineQuery(query_2)
        .build()
        .run();
  }

  @Test
  public void testSplit() throws Exception {
    testBuilder()
        .sqlQuery("select split(n_name, ' ') words from cp.`tpch/nation.parquet` where n_nationkey = 24")
        .unOrdered()
        .baselineColumns("words")
        .baselineValues(ImmutableList.of(new Text("UNITED"), new Text("STATES")))
        .build()
        .run();
  }

  @Test
  public void testSplitWithNullInput() throws Exception {
    // Contents of the generated file:
    /*
      {"a": "aaaaaa.bbb.cc.ddddd"}
      {"a": null}
      {"a": "aa"}
     */
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), "nullable_strings.json")))) {
      String[] fieldValue = {"\"aaaaaa.bbb.cc.ddddd\"", null, "\"aa\""};
      for (String value : fieldValue) {
        String entry = String.format("{ \"a\": %s}\n", value);
        writer.write(entry);
      }
    }

    testBuilder()
        .sqlQuery("select split(a, '.') wordsCount from dfs.`nullable_strings.json` t")
        .unOrdered()
        .baselineColumns("wordsCount")
        .baselineValues(ImmutableList.of(new Text("aaaaaa"), new Text("bbb"), new Text("cc"), new Text("ddddd")))
        .baselineValues(ImmutableList.of())
        .baselineValues(ImmutableList.of(new Text("aa")))
        .go();
  }

  @Test
  public void testReverse() throws Exception {
    testBuilder()
      .sqlQuery("select reverse('qwerty') words from (values(1))")
      .unOrdered()
      .baselineColumns("words")
      .baselineValues("ytrewq")
      .build()
      .run();
  }

  @Test // DRILL-5424
  public void testReverseLongVarChars() throws Exception {
    try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(dirTestWatcher.getRootDir(), "table_with_long_varchars.json")))) {
      for (int i = 0; i < 10; i++) {
        writer.write("{ \"a\": \"abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz\"}");
      }
    }

    test("select reverse(a) from dfs.`table_with_long_varchars.json` t");
  }

  @Test
  public void testLower() throws Exception {
    testBuilder()
        .sqlQuery("select\n" +
            "lower('ABC') col_upper,\n" +
            "lower('abc') col_lower,\n" +
            "lower('AbC aBc') col_space,\n" +
            "lower('123ABC$!abc123.') as col_special,\n" +
            "lower('') as col_empty,\n" +
            "lower(cast(null as varchar(10))) as col_null\n" +
            "from (values(1))")
        .unOrdered()
        .baselineColumns("col_upper", "col_lower", "col_space", "col_special", "col_empty", "col_null")
        .baselineValues("abc", "abc", "abc abc", "123abc$!abc123.", "", null)
        .build()
        .run();
  }

  @Test
  public void testUpper() throws Exception {
    testBuilder()
        .sqlQuery("select\n" +
            "upper('ABC')as col_upper,\n" +
            "upper('abc') as col_lower,\n" +
            "upper('AbC aBc') as col_space,\n" +
            "upper('123ABC$!abc123.') as col_special,\n" +
            "upper('') as col_empty,\n" +
            "upper(cast(null as varchar(10))) as col_null\n" +
            "from (values(1))")
        .unOrdered()
        .baselineColumns("col_upper", "col_lower", "col_space", "col_special", "col_empty", "col_null")
        .baselineValues("ABC", "ABC", "ABC ABC", "123ABC$!ABC123.", "", null)
        .build()
        .run();
  }

  @Test
  public void testInitcap() throws Exception {
    testBuilder()
        .sqlQuery("select\n" +
            "initcap('ABC')as col_upper,\n" +
            "initcap('abc') as col_lower,\n" +
            "initcap('AbC aBc') as col_space,\n" +
            "initcap('123ABC$!abc123.') as col_special,\n" +
            "initcap('') as col_empty,\n" +
            "initcap(cast(null as varchar(10))) as col_null\n" +
            "from (values(1))")
        .unOrdered()
        .baselineColumns("col_upper", "col_lower", "col_space", "col_special", "col_empty", "col_null")
        .baselineValues("Abc", "Abc", "Abc Abc", "123abc$!Abc123.", "", null)
        .build()
        .run();
  }

  @Test
  public void testMultiByteEncoding() throws Exception {
    testBuilder()
        .sqlQuery("select\n" +
            "upper('привет')as col_upper,\n" +
            "lower('ПРИВЕТ') as col_lower,\n" +
            "initcap('приВЕТ') as col_initcap\n" +
            "from (values(1))")
        .unOrdered()
        .baselineColumns("col_upper", "col_lower", "col_initcap")
        .baselineValues("ПРИВЕТ", "привет", "Привет")
        .build()
        .run();
  }
}
