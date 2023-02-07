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
package org.apache.drill.exec.store.kafka;

import org.apache.drill.categories.KafkaStorageTest;
import org.apache.drill.categories.SlowTest;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import static org.apache.drill.exec.store.kafka.TestKafkaSuit.NUM_JSON_MSG;
import static org.apache.drill.exec.store.kafka.TestKafkaSuit.embeddedKafkaCluster;
import static org.junit.Assert.assertEquals;

@Category({KafkaStorageTest.class, SlowTest.class})
public class KafkaFilterPushdownTest extends KafkaTestBase {
  private static final int NUM_PARTITIONS = 5;
  private static final String EXPECTED_PATTERN = "kafkaScanSpec.*\\n.*\"topicName\" : \"drill-pushdown-topic\"\\n(" +
    ".*\\n)?(.*\\n)?(.*\\n)?.*cost\"(.*\\n)(.*\\n).*outputRowCount\" : (%s.0)";

  @BeforeClass
  public static void setup() throws Exception {
    TestKafkaSuit.createTopicHelper(TestQueryConstants.JSON_PUSHDOWN_TOPIC, NUM_PARTITIONS);
    KafkaMessageGenerator generator = new KafkaMessageGenerator(embeddedKafkaCluster.getKafkaBrokerList(),
      StringSerializer.class);
    generator.populateJsonMsgWithTimestamps(TestQueryConstants.JSON_PUSHDOWN_TOPIC, NUM_JSON_MSG);
    String query = String.format(TestQueryConstants.MSG_SELECT_QUERY, TestQueryConstants.JSON_PUSHDOWN_TOPIC);
    //Ensure messages are present
    assertEquals("Kafka server does not have expected number of messages", testSql(query), NUM_PARTITIONS * NUM_JSON_MSG);
  }

  /**
   * Test filter pushdown with condition on kafkaMsgOffset.
   */
  @Test
  public void testPushdownOnOffset() throws Exception {
    final String predicate1 = "kafkaMsgOffset > 4";
    final String predicate2 = "kafkaMsgOffset < 6";
    final int expectedRowCount = 5; //1 * NUM_PARTITIONS

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_AND,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate1, predicate2);

    runKafkaSQLVerifyCount(queryString, expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown with condition on kafkaPartitionId.
   */
  @Test
  public void testPushdownOnPartition() throws Exception {
    final String predicate = "kafkaPartitionId = 1";
    final int expectedRowCount = NUM_JSON_MSG;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate);

    runKafkaSQLVerifyCount(queryString, expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown with condition on kafkaPartitionId.
   */
  @Test
  public void testPushdownOnTimestamp() throws Exception {
    final String predicate = "kafkaMsgTimestamp > 6";
    final int expectedRowCount = 20;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown when timestamp is not ordered.
   */
  @Test
  public void testPushdownUnorderedTimestamp() throws Exception {
    final String predicate = "kafkaMsgTimestamp = 1";
    final int expectedRowInPlan = 50;
    final int expectedRowCount = 5;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowInPlan))
      .match();
  }

  /**
   * Test filter pushdown when timestamp value specified does not exist.
   */
  @Test
  public void testPushdownWhenTimestampDoesNotExist() throws Exception {
    final String predicate = "kafkaMsgTimestamp = 20"; //20 does not exist
    final int expectedRowCount = 0;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown when partition value specified does not exist.
   */
  @Test
  public void testPushdownWhenPartitionDoesNotExist() throws Exception {
    final String predicate = "kafkaPartitionId = 100"; //100 does not exist
    final int expectedRowCount = 0;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown when timestamp exist but partition does not exist.
   */
  @Test
  public void testPushdownForEmptyScanSpec() throws Exception {
    final String predicate1 = "kafkaMsgTimestamp > 6";
    final String predicate2 = "kafkaPartitionId = 100";
    final int expectedRowCount = 0;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_AND,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate1, predicate2);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown on kafkaMsgOffset with boundary conditions.
   * In every case, the number of records returned is 0.
   */
  @Test
  public void testPushdownOffsetNoRecordsReturnedWithBoundaryConditions() throws Exception {
    final int expectedRowCount = 0;

    //"equal" such that value = endOffset
    String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset = 10");

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();

    //"equal" such that value < startOffset
    queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset = -1");

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();

    //"greater_than" such that value = endOffset-1
    queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset > 9");

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();

    //"greater_than_or_equal" such that value = endOffset
    queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset >= 10");

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();

    //"less_than" such that value = startOffset
    queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset < 0");

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();

    //"less_than_or_equal" such that value < startOffset
    queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset <= -1");

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown on kafkaMsgOffset with boundary conditions.
   * In every case, the number of records returned is 5 (1 per topic-partition).
   */
  @Test
  public void testPushdownOffsetOneRecordReturnedWithBoundaryConditions() throws Exception {
    final int expectedRowCount = 5;

    //"equal" such that value = endOffset-1
    String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset = 9");

    runKafkaSQLVerifyCount(queryString, expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();

    //"greater_than" such that value = endOffset-2
    queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset > 8");

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();

    //"greater_than_or_equal" such that value = endOffset-1
    queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_BASIC,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, "kafkaMsgOffset >= 9");

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown with OR.
   * Pushdown is possible if all the predicates are on metadata fields.
   */
  @Test
  public void testPushdownWithOr() throws Exception {
    final String predicate1 = "kafkaMsgTimestamp > 6";
    final String predicate2 = "kafkaPartitionId = 1";
    final int expectedRowCount = 26;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_OR,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate1, predicate2);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test filter pushdown with OR on kafkaMsgTimestamp and kafkaMsgOffset.
   */
  @Test
  public void testPushdownWithOr1() throws Exception {
    final String predicate1 = "kafkaMsgTimestamp = 6";
    final String predicate2 = "kafkaMsgOffset = 6";
    final int expectedRowInPlan = 25; //startOff=5, endOff=9
    final int expectedRowCount = 10;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_OR,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate1, predicate2);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowInPlan))
      .match();
  }

  /**
   * Test pushdown for a combination of AND and OR.
   */
  @Test
  public void testPushdownWithAndOrCombo() throws Exception {
    final String predicate1 = "kafkaMsgTimestamp > 6";
    final String predicate2 = "kafkaPartitionId = 1";
    final String predicate3 = "kafkaPartitionId = 2";
    final int expectedRowCount = 8;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_AND_OR_PATTERN_1,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate1, predicate2, predicate3);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCount))
      .match();
  }

  /**
   * Test pushdown for a combination of AND and OR.
   */
  @Test
  public void testPushdownWithAndOrCombo2() throws Exception {
    final String predicate1 = "kafkaMsgTimestamp = 6";
    final String predicate2 = "kafkaMsgOffset = 6";
    final String predicate3 = "kafkaPartitionId = 1";
    final String predicate4 = "kafkaPartitionId = 2";
    final int expectedRowCountInPlan = 10; //startOff=5, endOff=9 for 2 partitions
    final int expectedRowCount = 4;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_AND_OR_PATTERN_3,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate1, predicate2, predicate3, predicate4);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCountInPlan))
      .match();
  }

  /**
   * Test pushdown for predicate1 AND predicate2.
   * Where predicate1 is on metadata field and and predicate2 is on user fields.
   */
  @Test
  public void testPushdownTimestampWithNonMetaField() throws Exception {
    final String predicate1 = "kafkaMsgTimestamp > 6";
    final String predicate2 = "boolKey = true";
    final int expectedRowCountInPlan = 20; //startOff=5, endOff=9
    final int expectedRowCount = 10;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_AND,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate1, predicate2);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCountInPlan))
      .match();
  }

  /**
   * Tests that pushdown does not happen for predicates such as
   * non-metadata-field = val1 OR (kafkaMsgTimestamp > val2 AND kafkaMsgTimestamp < val4)
   */
  @Test
  public void testNoPushdownOfOffsetWithNonMetadataField() throws Exception {
    final String predicate1 = "boolKey = true";
    final String predicate2 = "kafkaMsgTimestamp > 6";
    final String predicate3 = "kafkaMsgTimestamp < 9";
    final int expectedRowCountInPlan = 50; //no pushdown
    final int expectedRowCount = 30;

    final String queryString = String.format(TestQueryConstants.QUERY_TEMPLATE_AND_OR_PATTERN_2,
      TestQueryConstants.JSON_PUSHDOWN_TOPIC, predicate1, predicate2, predicate3);

    runKafkaSQLVerifyCount(queryString,expectedRowCount);
    queryBuilder()
      .sql(queryString)
      .jsonPlanMatcher()
      .include(String.format(EXPECTED_PATTERN, expectedRowCountInPlan))
      .match();
  }
}
