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
package org.apache.drill.exec.store.easy.text.compliant;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.common.exceptions.UserRemoteException;
import org.apache.drill.exec.proto.UserBitShared.DrillPBError.ErrorType;
import org.apache.drill.test.BaseTestQuery;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;

public class TestTextReader extends BaseTestQuery {

  @Test
  public void fieldDelimiterWithinQuotes() throws Exception {
    testBuilder()
        .sqlQuery("select columns[1] as col1 from cp.`textinput/input1.csv`")
        .unOrdered()
        .baselineColumns("col1")
        .baselineValues("foo,bar")
        .go();
  }

  @Ignore ("Not needed any more. (DRILL-3178)")
  @Test
  public void ensureFailureOnNewLineDelimiterWithinQuotes() {
    try {
      test("select columns[1] as col1 from cp.`textinput/input2.csv`");
      fail("Expected exception not thrown.");
    } catch (Exception e) {
      assertTrue(e.getMessage().contains("Cannot use newline character within quoted string"));
    }
  }

  @Test
  public void ensureColumnNameDisplayedinError() throws Exception {
    final String COL_NAME = "col1";

    try {
      test("select max(columns[1]) as %s from cp.`textinput/input1.csv` where %s is not null", COL_NAME, COL_NAME);
      fail("Query should have failed");
    } catch(UserRemoteException ex) {
      assertEquals(ErrorType.VALIDATION, ex.getErrorType());
    }
  }

  @Test // see DRILL-3718
  @Category(UnlikelyTest.class)
  public void testTabSeparatedWithQuote() throws Exception {
    testBuilder()
        .sqlQuery("select columns[0] as c0, columns[1] as c1, columns[2] as c2 \n" +
          "from cp.`store/text/WithQuote.tsv`")
        .unOrdered()
        .baselineColumns("c0", "c1", "c2")
        .baselineValues("a", "a", "a")
        .baselineValues("a", "a", "a")
        .baselineValues("a", "a", "a")
        .build()
        .run();
  }

  @Test // see DRILL-3718
  @Category(UnlikelyTest.class)
  public void testSpaceSeparatedWithQuote() throws Exception {
    testBuilder()
        .sqlQuery("select columns[0] as c0, columns[1] as c1, columns[2] as c2 \n" +
          "from cp.`store/text/WithQuote.ssv`")
        .unOrdered()
        .baselineColumns("c0", "c1", "c2")
        .baselineValues("a", "a", "a")
        .baselineValues("a", "a", "a")
        .baselineValues("a", "a", "a")
        .build()
        .run();
  }

  @Test // see DRILL-3718
  @Category(UnlikelyTest.class)
  public void testPipSeparatedWithQuote() throws Exception {
    testBuilder()
        .sqlQuery("select columns[0] as c0, columns[1] as c1, columns[2] as c2 \n" +
          "from cp.`store/text/WithQuote.tbl`")
        .unOrdered()
        .baselineColumns("c0", "c1", "c2")
        .baselineValues("a", "a", "a")
        .baselineValues("a", "a", "a")
        .baselineValues("a", "a", "a")
        .build()
        .run();
  }

  @Test // see DRILL-3718
  @Category(UnlikelyTest.class)
  public void testCrLfSeparatedWithQuote() throws Exception {
    testBuilder()
        .sqlQuery("select columns[0] as c0, columns[1] as c1, columns[2] as c2 \n" +
          "from cp.`store/text/WithQuotedCrLf.tbl`")
        .unOrdered()
        .baselineColumns("c0", "c1", "c2")
        .baselineValues("a\n1", "a", "a")
        .baselineValues("a", "a\n2", "a")
        .baselineValues("a", "a", "a\n3")
        .build()
        .run();
  }
}
