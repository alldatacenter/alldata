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
package org.apache.drill.exec.planner.common;

import org.apache.drill.categories.PlannerTest;

import org.apache.drill.shaded.guava.com.google.common.collect.BoundType;
import org.apache.drill.test.BaseTest;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.Assert;
import org.apache.drill.shaded.guava.com.google.common.collect.Range;


@Category(PlannerTest.class)
public class TestNumericEquiDepthHistogram extends BaseTest {

  @Test
  public void testHistogramWithUniqueEndpoints() throws Exception {
    int numBuckets = 10;
    int numRowsPerBucket = 250;
    long ndv = 25;

    // init array with numBuckets + 1 values
    Double[] buckets = {1.0, 10.0, 20.0, 30.0, 40.0, 50.0, 60.0, 70.0, 80.0, 90.0, 100.0};

    NumericEquiDepthHistogram histogram = new NumericEquiDepthHistogram(numBuckets);

    for (int i = 0; i < buckets.length; i++) {
      histogram.setBucketValue(i, buckets[i]);
    }
    histogram.setNumRowsPerBucket(numRowsPerBucket);

    // Range: <= 1.0
    Range<Double> range1 = Range.atMost(new Double(1.0));
    long result1 = histogram.getSelectedRows(range1, ndv);
    long expected1 = 10;
    Assert.assertEquals(expected1, result1);

    // Range: >= 100.0
    Range<Double> range2 = Range.atLeast(new Double(100.0));
    long result2 = histogram.getSelectedRows(range2, ndv);
    long expected2 = 10;
    Assert.assertEquals(expected2, result2);
  }

  @Test
  public void testHistogramWithDuplicateEndpoints() throws Exception {
    int numBuckets = 10;
    int numRowsPerBucket = 250;
    long ndv = 25;

    // init array with numBuckets + 1 values
    Double[] buckets = {10.0, 10.0, 10.0, 20.0, 20.0, 50.0, 55.0, 55.0, 60.0, 100.0, 100.0};

    NumericEquiDepthHistogram histogram = new NumericEquiDepthHistogram(numBuckets);

    for (int i = 0; i < buckets.length; i++) {
      histogram.setBucketValue(i, buckets[i]);
    }
    histogram.setNumRowsPerBucket(numRowsPerBucket);

    // Range: <= 10.0
    Range<Double> range1 = Range.atMost(new Double(10.0));
    long result1 = histogram.getSelectedRows(range1, ndv);
    long expected1 = 510; // 2 full buckets plus the exact match with start point of 3rd bucket
    Assert.assertEquals(expected1, result1);

    // Range: >= 100.0
    Range<Double> range2 = Range.atLeast(new Double(100.0));
    long result2 = histogram.getSelectedRows(range2, ndv);
    long expected2 = 250;
    Assert.assertEquals(expected2, result2);

    // Range: < 10.0
    Range<Double> range3 = Range.lessThan(new Double(10.0));
    long result3 = histogram.getSelectedRows(range3, ndv);
    long expected3 = 0;
    Assert.assertEquals(expected3, result3);

    // Range: > 100.0
    Range<Double> range4 = Range.greaterThan(new Double(100.0));
    long result4 = histogram.getSelectedRows(range4, ndv);
    long expected4 = 0;
    Assert.assertEquals(expected4, result4);

    // Range: >= 20.0 AND <= 55.0
    Range<Double> range5 = Range.range(new Double(20.0), BoundType.CLOSED, new Double(55.0), BoundType.CLOSED);
    long result5 = histogram.getSelectedRows(range5, ndv);
    long expected5 = 1010;
    Assert.assertEquals(expected5, result5);

    // Range: BETWEEN 15 AND 80
    Range<Double> range6 = Range.range(new Double(15.0), BoundType.CLOSED, new Double(80.0), BoundType.CLOSED);
    long result6 = histogram.getSelectedRows(range6, ndv);
    long expected6 = 1500;
    Assert.assertEquals(expected6, result6);
  }
}
