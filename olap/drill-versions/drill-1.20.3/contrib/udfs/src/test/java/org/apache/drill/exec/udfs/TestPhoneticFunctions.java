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
package org.apache.drill.exec.udfs;

import org.apache.drill.categories.SqlFunctionTest;
import org.apache.drill.categories.UnlikelyTest;
import org.apache.drill.test.ClusterFixture;
import org.apache.drill.test.ClusterFixtureBuilder;
import org.apache.drill.test.ClusterTest;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.junit.Assert.assertEquals;

@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestPhoneticFunctions extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testSoundex() throws Exception {
    String result = queryBuilder()
        .sql("select soundex('jaime') as soundex from (values(1))")
        .singletonString();
    assertEquals("J500", result);
  }

  @Test
  public void testCaverphone1() throws Exception {
    String result = queryBuilder()
        .sql("SELECT caverphone1('jaime') as caverphone FROM (VALUES(1))")
        .singletonString();
    assertEquals("YM1111", result);
  }

  @Test
  public void testCaverphone2() throws Exception {
    String result = queryBuilder()
        .sql("SELECT caverphone2('steve') as caverphone FROM (VALUES(1))")
        .singletonString();
    assertEquals("STF1111111", result);
  }

  @Test
  public void testCologne() throws Exception {
    String result = queryBuilder()
        .sql("SELECT cologne_phonetic('steve') AS CP FROM (VALUES(1))")
        .singletonString();
    assertEquals("823", result);
  }

  @Test
  public void testMatchRatingEncoder() throws Exception {
    String result = queryBuilder()
        .sql("SELECT match_rating_encoder('Boston') AS MR FROM (VALUES(1))")
        .singletonString();
    assertEquals("BSTN", result);
  }

  @Test
  public void testNYSIIS() throws Exception {
    String result = queryBuilder()
        .sql("SELECT nysiis('Boston') AS ny FROM (VALUES(1))")
        .singletonString();
    assertEquals("BASTAN", result);
  }

  @Test
  public void testRefinedSoundex() throws Exception {
    String result = queryBuilder()
        .sql("SELECT refined_soundex('Boston') AS rs FROM (VALUES(1))")
        .singletonString();
    assertEquals("B103608", result);
  }

  @Test
  public void testMetaphone() throws Exception {
    String result = queryBuilder()
        .sql("SELECT metaphone('Phoenix') AS meta FROM (VALUES(1))")
        .singletonString();
    assertEquals("FNKS", result);
  }

  @Test
  public void testDoubleMetaphone() throws Exception {
    String result = queryBuilder()
        .sql("SELECT double_metaphone('Phoenix') AS meta FROM (VALUES(1))")
        .singletonString();
    assertEquals("FNKS", result);

    result = queryBuilder()
        .sql("SELECT double_metaphone('') AS meta FROM (VALUES(1))")
        .singletonString();
    assertEquals("", result);
  }
}
