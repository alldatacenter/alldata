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
public class TestStringDistanceFunctions extends ClusterTest {

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testCosineDistance() throws Exception {
    double result = queryBuilder()
        .sql("select cosine_distance( 'Big car', 'red car' ) as distance FROM (VALUES(1))")
        .singletonDouble();
    assertEquals(0.5000000000000001, result, 0.0);
  }

  @Test
  public void testHammingDistance() throws Exception {
    double result = queryBuilder()
        .sql("select hamming_distance( 'Big car', 'red car' ) as distance FROM (VALUES(1))")
        .singletonDouble();
    assertEquals(3.0, result, 0.0);
  }

  @Test
  public void testJaccardDistance() throws Exception {
    double result = queryBuilder()
        .sql("select jaccard_distance( 'Big car', 'red car' ) as distance FROM (VALUES(1))")
        .singletonDouble();
    assertEquals(0.5555555555555556, result, 0.0);
  }

  @Test
  public void testJaroDistance() throws Exception {
    double result = queryBuilder()
        .sql("select jaro_distance( 'Big car', 'red car' ) as distance FROM (VALUES(1))")
        .singletonDouble();
    assertEquals(0.2857142857142857, result, 0.0);
  }

  @Test
  public void testLevenshteinDistance() throws Exception {
    double result = queryBuilder()
        .sql("select levenshtein_distance( 'Big car', 'red car' ) as distance FROM (VALUES(1))")
        .singletonDouble();
    assertEquals(3.0, result, 0.0);
  }
}
