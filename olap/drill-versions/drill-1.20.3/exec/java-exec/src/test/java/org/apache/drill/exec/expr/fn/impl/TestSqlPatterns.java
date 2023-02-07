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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;

import org.apache.drill.common.exceptions.DrillRuntimeException;
import org.apache.drill.exec.memory.BufferAllocator;
import org.apache.drill.exec.memory.RootAllocatorFactory;
import org.apache.drill.test.BaseTest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import io.netty.buffer.DrillBuf;

public class TestSqlPatterns extends BaseTest {
  BufferAllocator allocator;
  DrillBuf drillBuf;
  CharsetEncoder charsetEncoder;
  CharBuffer charBuffer;
  ByteBuffer byteBuffer;

  String wideString = "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" +
      "AFyMJ4kLsONE52kZsJYYyDpWw9a8BZ";

  @Before
  public void setup() {
    allocator = RootAllocatorFactory.newRoot(16384);
    drillBuf = allocator.buffer(8192);
    charsetEncoder = Charset.forName("UTF-8").newEncoder();
  }

  @Test
  public void testSqlRegexLike() {
    // Given SQL like pattern, verify patternType is correct.
    // Java pattern should have % replaced with .*, _ replaced with .
    // Simple pattern should have meta (% and _) and escape characters removed.

    // A%B is complex
    RegexpUtil.SqlPatternInfo patternInfo = RegexpUtil.sqlToRegexLike("A%B");
    assertEquals("A.*B", patternInfo.getJavaPatternString());
    assertEquals(RegexpUtil.SqlPatternType.COMPLEX, patternInfo.getPatternType());

    // A_B is complex
    patternInfo = RegexpUtil.sqlToRegexLike("A_B");
    assertEquals("A.B", patternInfo.getJavaPatternString());
    assertEquals(RegexpUtil.SqlPatternType.COMPLEX, patternInfo.getPatternType());

    // A%B%D is complex
    patternInfo = RegexpUtil.sqlToRegexLike("A%B%D");
    assertEquals("A.*B.*D", patternInfo.getJavaPatternString());
    assertEquals(RegexpUtil.SqlPatternType.COMPLEX, patternInfo.getPatternType());

    // %AB% is contains
    patternInfo = RegexpUtil.sqlToRegexLike("%AB%");
    assertEquals(".*AB.*", patternInfo.getJavaPatternString());
    assertEquals("AB", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.CONTAINS, patternInfo.getPatternType());

    // %AB is ends with
    patternInfo = RegexpUtil.sqlToRegexLike("%AB");
    assertEquals(".*AB", patternInfo.getJavaPatternString());
    assertEquals("AB", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.ENDS_WITH, patternInfo.getPatternType());

    // AB% is starts with
    patternInfo = RegexpUtil.sqlToRegexLike("AB%");
    assertEquals("AB.*", patternInfo.getJavaPatternString());
    assertEquals("AB", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.STARTS_WITH, patternInfo.getPatternType());

    // AB is constant.
    patternInfo = RegexpUtil.sqlToRegexLike("AB");
    assertEquals("AB", patternInfo.getJavaPatternString());
    assertEquals("AB", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.CONSTANT, patternInfo.getPatternType());

    // A.B is constant. DRILL-8278
    patternInfo = RegexpUtil.sqlToRegexLike("A.B");
    // The . should be escaped with a \ so that it represents a literal .
    assertEquals("A\\.B", patternInfo.getJavaPatternString());
    assertEquals("A.B", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.CONSTANT, patternInfo.getPatternType());

    // Test with escape characters.

    // A%#B is invalid escape sequence
    try {
      patternInfo = RegexpUtil.sqlToRegexLike("A%#B", '#');
    } catch (Exception ex) {
      assertTrue(ex.getMessage().contains("Invalid escape sequence"));
    }

    // A#%B with # as escape character is constant A%B
    patternInfo = RegexpUtil.sqlToRegexLike("A#%B", '#');
    assertEquals("A%B", patternInfo.getJavaPatternString());
    assertEquals("A%B", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.CONSTANT, patternInfo.getPatternType());

    // %A#%B% is contains A%B
    patternInfo = RegexpUtil.sqlToRegexLike("%A#%B%", '#');
    assertEquals(".*A%B.*", patternInfo.getJavaPatternString());
    assertEquals("A%B", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.CONTAINS, patternInfo.getPatternType());

    // #%AB% is starts with %AB
    patternInfo = RegexpUtil.sqlToRegexLike("#%AB%", '#');
    assertEquals("%AB.*", patternInfo.getJavaPatternString());
    assertEquals("%AB", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.STARTS_WITH, patternInfo.getPatternType());

    // %#%AB#% is ends with %AB%
    patternInfo = RegexpUtil.sqlToRegexLike("%#%AB#%", '#');
    assertEquals(".*%AB%", patternInfo.getJavaPatternString());
    assertEquals("%AB%", patternInfo.getSimplePatternString());
    assertEquals(RegexpUtil.SqlPatternType.ENDS_WITH, patternInfo.getPatternType());

    // #_A#%B%C is complex
    patternInfo = RegexpUtil.sqlToRegexLike("#_A#%B%C", '#');
    assertEquals("_A%B.*C", patternInfo.getJavaPatternString());
    assertEquals(RegexpUtil.SqlPatternType.COMPLEX, patternInfo.getPatternType());

  }

  private void setDrillBuf(String input) {
    drillBuf.clear();
    charBuffer = CharBuffer.wrap(input);
    try {
      byteBuffer = charsetEncoder.encode(charBuffer);
    } catch (CharacterCodingException e) {
      throw new DrillRuntimeException("Error while encoding the pattern string ", e);
    }
    drillBuf.setBytes(0, byteBuffer, byteBuffer.position(), byteBuffer.remaining());
  }

  @Test
  public void testSqlPatternStartsWith() {
    RegexpUtil.SqlPatternInfo patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.STARTS_WITH, "", "ABC");
    SqlPatternMatcher sqlPatternStartsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("ABCD");
    assertEquals(1, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf));  // ABCD should match StartsWith ABC

    setDrillBuf("BCD");
    assertEquals(0, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf));  // BCD should not match StartsWith ABC

    setDrillBuf("XYZABC");
    assertEquals(0, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf));  // XYZABC should not match StartsWith ABC

    // null text
    setDrillBuf("");
    assertEquals(0, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf)); // null String should not match StartsWith ABC

    // pattern length > txt length
    setDrillBuf("AB");
    assertEquals(0, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf)); // AB should not match StartsWith ABC

    // startsWith null pattern should match anything
    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.STARTS_WITH, "", "");
    sqlPatternStartsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("AB");
    assertEquals(1, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf)); // AB should match StartsWith null pattern

    // null pattern and null text
    setDrillBuf("");
    assertEquals(1, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf)); // null text should match null pattern

    // wide character string.
    setDrillBuf(wideString);

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.STARTS_WITH, "", "b00dUrA0oa2i4ZEHg6zvPXPXlVQYB2BXe8T5gIEtvUDzcN6yUkIqyS07gaAy8k4");
    sqlPatternStartsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf)); // should match

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.STARTS_WITH, "", "AFyMJ4kLsONE52kZsJYYyDpWw9a8BZ");
    sqlPatternStartsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(0, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf)); // should not match

    // non ascii
    setDrillBuf("¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~");
    assertEquals(0, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf)); // should not match

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.STARTS_WITH, "", "¤EÀsÆW");
    sqlPatternStartsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    setDrillBuf("¤EÀsÆW");
    assertEquals(1, sqlPatternStartsWith.match(0, byteBuffer.limit(), drillBuf)); // should match
  }

  @Test
  public void testSqlPatternEndsWith() {
    RegexpUtil.SqlPatternInfo patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.ENDS_WITH, "", "BCD");
    SqlPatternMatcher sqlPatternEndsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("ABCD");
    assertEquals(1, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf));  // ABCD should match EndsWith BCD

    setDrillBuf("ABC");
    assertEquals(0, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf));  // ABC should not match EndsWith BCD

    setDrillBuf("");
    assertEquals(0, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf));  // null string should not match EndsWith BCD

    setDrillBuf("A");
    assertEquals(0, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf)); // ABCD should not match EndsWith A

    setDrillBuf("XYZBCD");
    assertEquals(1, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf));  // XYZBCD should match EndsWith BCD

    // EndsWith null pattern should match anything
    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.ENDS_WITH, "", "");
    sqlPatternEndsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf)); // AB should match StartsWith null pattern

    // null pattern and null text
    setDrillBuf("");
    assertEquals(1, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf)); // null text should match null pattern

    // wide character string.
    setDrillBuf(wideString);

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.ENDS_WITH, "", "AFyMJ4kLsONE52kZsJYYyDpWw9a8BZ");
    sqlPatternEndsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf)); // should match

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.ENDS_WITH, "", "");
    sqlPatternEndsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("");
    assertEquals(1, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf)); // null text should match null pattern

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.ENDS_WITH, "", "atXzj4x3CgF6j1gn10aUJknF7KQLJ84D");
    sqlPatternEndsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(0, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf)); // should not match

    // non ascii
    setDrillBuf("¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~");
    assertEquals(0, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf)); // should not match

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.ENDS_WITH, "", "TÆU2~~");
    sqlPatternEndsWith = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternEndsWith.match(0, byteBuffer.limit(), drillBuf)); // should match
  }

  @Test
  public void testSqlPatternContains() {
    RegexpUtil.SqlPatternInfo patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONTAINS, ".*ABC.*", "ABCD");
    SqlPatternMatcher sqlPatternContains = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("ABCD");
    assertEquals(1, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf));  // ABCD should contain ABCD

    setDrillBuf("BC");
    assertEquals(0, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf));  // BC cannot contain ABCD

    setDrillBuf("");
    assertEquals(0, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf));  // null string should not match contains ABCD

    setDrillBuf("DE");
    assertEquals(0, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf));  // ABCD should not contain DE

    setDrillBuf("xyzABCDqrs");
    assertEquals(1, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf));   // xyzABCDqrs should contain ABCD

    // contains null pattern should match anything
    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONTAINS, "", "");
    sqlPatternContains = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("xyzABCDqrs");
    assertEquals(1, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf)); //  should match

    // null pattern and null text
    setDrillBuf("");
    assertEquals(1, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf)); // null text should match null pattern

    // wide character string.
    setDrillBuf(wideString);

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONTAINS, "", "tLsQ1gYWPF1geDalS5MdWfpDvF5MaeJMd2fK0m3jui7xY1IfuSxqZs7SEL6wUVGdWc5tsVroCMMy6Nqjdz5T4vW");
    sqlPatternContains = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    assertEquals(1, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf));

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONTAINS, "", "ABCDEF");
    sqlPatternContains = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(0, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf));

    // non ascii
    setDrillBuf("¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~");
    assertEquals(0, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf)); // should not match

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONTAINS, "", "¶T¤¤¤ß");
    sqlPatternContains = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternContains.match(0, byteBuffer.limit(), drillBuf)); // should match
  }

  @Test
  public void testSqlPatternConstant() {
    RegexpUtil.SqlPatternInfo patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONSTANT, "ABC.*", "ABC");
    SqlPatternMatcher sqlPatternConstant = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("ABC");
    assertEquals(1, sqlPatternConstant.match(0, byteBuffer.limit(), drillBuf));  // ABC should match ABC

    setDrillBuf("BC");
    assertEquals(0, sqlPatternConstant.match(0, byteBuffer.limit(), drillBuf));  // ABC not same as BC

    setDrillBuf("");
    assertEquals(0, sqlPatternConstant.match(0, byteBuffer.limit(), drillBuf));  // null string not same as ABC

    setDrillBuf("DE");
    assertEquals(0, sqlPatternConstant.match(0, byteBuffer.limit(), drillBuf));  // ABC not same as DE

    // null pattern should match null string
    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONSTANT, "", "");
    sqlPatternConstant = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("");
    assertEquals(1, sqlPatternConstant.match(0, byteBuffer.limit(), drillBuf)); // null text should match null pattern

    // wide character string.
    setDrillBuf(wideString);

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONTAINS, "", wideString);
    sqlPatternConstant = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternConstant.match(0, byteBuffer.limit(), drillBuf));

    // non ascii
    setDrillBuf("¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~");
    assertEquals(0, sqlPatternConstant.match(0, byteBuffer.limit(), drillBuf));  // should not match

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONSTANT, "", "¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~");
    sqlPatternConstant = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternConstant.match(0, byteBuffer.limit(), drillBuf)); // should match
  }

  @Test
  public void testSqlPatternComplex() {
    RegexpUtil.SqlPatternInfo patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.COMPLEX, "A.*BC.*", "");
    SqlPatternMatcher sqlPatternComplex = SqlPatternFactory.getSqlPatternMatcher(patternInfo);

    setDrillBuf("ABCDEF");
    assertEquals(1, sqlPatternComplex.match(0, byteBuffer.limit(), drillBuf));  // ADEBCDF should match A.*BC.*

    setDrillBuf("BC");
    assertEquals(0, sqlPatternComplex.match(0, byteBuffer.limit(), drillBuf));  // BC should not match A.*BC.*

    setDrillBuf("");
    assertEquals(sqlPatternComplex.match(0, byteBuffer.limit(), drillBuf), 0);  // null string should not match

    setDrillBuf("DEFGHIJ");
    assertEquals(sqlPatternComplex.match(0, byteBuffer.limit(), drillBuf), 0); // DEFGHIJ should not match A.*BC.*

    java.util.regex.Matcher matcher;
    matcher = java.util.regex.Pattern.compile("b00dUrA0.*").matcher("");

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.COMPLEX, "b00dUrA0.*42.*9a8BZ", "");

    // wide character string.
    setDrillBuf(wideString);

    sqlPatternComplex = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternComplex.match(0, byteBuffer.limit(), drillBuf));

    // non ascii
    setDrillBuf("¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~");
    assertEquals(0, sqlPatternComplex.match(0, byteBuffer.limit(), drillBuf)); // DEFGHIJ should not match A.*BC.*

    patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.COMPLEX, ".*»Ú®i¶T¤¤¤.*¼Ó®i.*ÆU2~~", "");
    sqlPatternComplex = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
    assertEquals(1, sqlPatternComplex.match(0, byteBuffer.limit(), drillBuf)); // should match
  }

  @Test
  public void testSqlPatternContainsMultipleMatchers() {

    final String longASCIIString = "Drill supports a variety of NoSQL databases and file systems, including HBase, MongoDB, MapR-DB, HDFS, MapR-FS, Amazon S3, Azure Blob Storage, Google Cloud Storage, Swift, "
      + "NAS and local files. A single query can join data from multiple datastores. For example, you can join a user profile collection in MongoDB with a directory of event logs in Hadoop.";
    final String emptyString     = "";
    final String unicodeString   = "¤EÀsÆW°ê»Ú®i¶T¤¤¤ß3¼Ó®i¶TÆU2~~";

    final List<SQLPatternTestParams> tests = new ArrayList<SQLPatternTestParams>();

    // Tests for Matcher ZERO
    tests.add(new SQLPatternTestParams(longASCIIString, "", true));
    tests.add(new SQLPatternTestParams(emptyString,     "", true));
    tests.add(new SQLPatternTestParams(unicodeString,   "", true));

    // Tests for Matcher ONE
    tests.add(new SQLPatternTestParams(longASCIIString, "N", true));
    tests.add(new SQLPatternTestParams(longASCIIString, "&", false));
    tests.add(new SQLPatternTestParams(emptyString,     "N", false));

    // Tests for Matcher TWO
    tests.add(new SQLPatternTestParams(longASCIIString, "SQ", true));
    tests.add(new SQLPatternTestParams(longASCIIString, "eT", false));
    tests.add(new SQLPatternTestParams("A",             "SQ", false));
    tests.add(new SQLPatternTestParams(emptyString,     "SQ", false));
    tests.add(new SQLPatternTestParams(unicodeString,   "¶",  true));
    tests.add(new SQLPatternTestParams(unicodeString,   "AT", false));

    // Tests for Matcher THREE
    tests.add(new SQLPatternTestParams(longASCIIString, "SQL", true));
    tests.add(new SQLPatternTestParams(longASCIIString, "cas", false));
    tests.add(new SQLPatternTestParams("S",             "SQL", false));
    tests.add(new SQLPatternTestParams(emptyString,     "SQL", false));
    tests.add(new SQLPatternTestParams(unicodeString,   "¶T", true));
    tests.add(new SQLPatternTestParams(unicodeString,   "¶A", false));

    // Tests for Matcher for patterns of length: 3 < length < 10
    tests.add(new SQLPatternTestParams(longASCIIString, "MongoDB", true));
    tests.add(new SQLPatternTestParams(longASCIIString, "MongoDz", false));
    tests.add(new SQLPatternTestParams("Mon",           "MongoDB", false));
    tests.add(new SQLPatternTestParams(emptyString,     "MongoDB", false));
    tests.add(new SQLPatternTestParams(unicodeString,   "®i¶", true));
    tests.add(new SQLPatternTestParams(unicodeString,   "®x¶", false));

    // Tests for Matcher for patterns of length >= 10
    tests.add(new SQLPatternTestParams(longASCIIString, "multiple datastores", true));
    tests.add(new SQLPatternTestParams(longASCIIString, "multiple datastorb",  false));
    tests.add(new SQLPatternTestParams("multiple",      "multiple datastores", false));
    tests.add(new SQLPatternTestParams(emptyString,     "multiple datastores", false));
    tests.add(new SQLPatternTestParams(unicodeString,   "¶T¤¤¤ß3¼", true));
    tests.add(new SQLPatternTestParams(unicodeString,   "¶T¤¤¤ßz¼", false));

    for (SQLPatternTestParams test : tests) {
      setDrillBuf(test.inputString);

      RegexpUtil.SqlPatternInfo patternInfo = new RegexpUtil.SqlPatternInfo(RegexpUtil.SqlPatternType.CONTAINS, "", test.patternString);
      SqlPatternMatcher sqlPatternContains  = SqlPatternFactory.getSqlPatternMatcher(patternInfo);
      int eval                              = sqlPatternContains.match(0, byteBuffer.limit(), drillBuf);
      int expectedEval                      = test.shouldMatch ? 1 : 0;

      if (eval != expectedEval) {
        System.err.format("test failed; params=%s%n", test);
      }

      assertEquals(expectedEval, eval);
    }
  }


  @After
  public void cleanup() {
    drillBuf.close();
    allocator.close();
  }

  // -------------
  // Inner Classes
  // -------------

  /** Container class to hold SQL pattern test data */
  private static class SQLPatternTestParams {
    private final String inputString;
    private final String patternString;
    private final boolean shouldMatch;

    private SQLPatternTestParams(String inputString, String patternString, boolean shouldMatch) {
      this.inputString   = inputString;
      this.patternString = patternString;
      this.shouldMatch   = shouldMatch;
    }

    public String toString() {
      return "input=["+inputString+"], pattern=["+patternString+"], should-match=["+shouldMatch+"]..";
    }
  }
}

