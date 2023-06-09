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

import java.time.LocalDateTime;


@Category({UnlikelyTest.class, SqlFunctionTest.class})
public class TestTimeBucketFunction extends ClusterTest {

  // Friday, January 1, 2016 12:06:00 AM
  private final static long MILLISECOND_TIMESTAMP = 1451606760L;

  // Friday, January 1, 2016 12:06:00 AM
  private final static long NAMOSECOND_TIMESTAMP = 1451606760000000000L;

  // Friday, March 27, 2020 1:33:53.845 AM
  private final static long MARCH27_TIMESTAMP = 1585272833845L;

  private final static int INTERVAL = 300000;

  @BeforeClass
  public static void setup() throws Exception {
    ClusterFixtureBuilder builder = ClusterFixture.builder(dirTestWatcher);
    startCluster(builder);
  }

  @Test
  public void testTimeBucketNanoSeconds() throws Exception {
    String query = String.format("SELECT time_bucket_ns(%d, %d) AS high", NAMOSECOND_TIMESTAMP, INTERVAL);
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues(1451606700000000000L)
      .go();
  }

  @Test
  public void testNullTimeBucketNanoSeconds() throws Exception {
    String query = String.format("SELECT time_bucket_ns(null, %d) AS high", INTERVAL);
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues((Long) null)
      .go();
  }

  @Test
  public void testNullIntervalTimeBucketNanoSeconds() throws Exception {
    String query = String.format("SELECT time_bucket_ns(%d, null) AS high", NAMOSECOND_TIMESTAMP);
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues((Long) null)
      .go();
  }

  @Test
  public void testBothNullIntervalTimeBucketNanoSeconds() throws Exception {
    String query = "SELECT time_bucket_ns(null, null) AS high";
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues((Long) null)
      .go();
  }

  @Test
  public void testTimeBucket() throws Exception {
    String query = String.format("SELECT time_bucket(%d, 300000) AS high", MILLISECOND_TIMESTAMP);
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues(1451400000L)
      .go();
  }

  @Test
  public void testDoubleTimeBucket() throws Exception {
    String query = String.format("SELECT time_bucket(CAST(%d AS DOUBLE), 300000) AS high", MILLISECOND_TIMESTAMP);
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues(1451400000L)
      .go();
  }

  @Test
  public void testTimeBucketTimestamp() throws Exception {
    String query = String.format("SELECT time_bucket(CAST( %d AS TIMESTAMP), 300000) AS high", MARCH27_TIMESTAMP);
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues(LocalDateTime.of(2020, 3, 27, 1, 30, 0))
      .go();
  }

  @Test
  public void testNullTimeBucket() throws Exception {
    String query = String.format("SELECT time_bucket(null, %d) AS high", INTERVAL);
    testBuilder()
      .sqlQuery(query)
      .ordered()
      .baselineColumns("high")
      .baselineValues((Long) null)
      .go();
  }

  @Test
  public void testNullIntervalTimeBucket() throws Exception {
    String query = String.format("SELECT time_bucket(%d, null) AS high", MILLISECOND_TIMESTAMP);
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues((Long) null)
      .go();
  }

  @Test
  public void testBothNullIntervalTimeBucket() throws Exception {
    String query = "SELECT time_bucket(null, null) AS high";
    testBuilder()
      .sqlQuery(query)
      .unOrdered()
      .baselineColumns("high")
      .baselineValues((Long) null)
      .go();
  }
}
