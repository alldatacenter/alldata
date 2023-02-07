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
package org.apache.drill.exec.physical.impl.join;

import org.apache.drill.exec.vector.IntVector;
import org.apache.drill.test.BaseTest;
import org.junit.Assert;
import org.junit.Test;

public class TestBatchSizePredictorImpl extends BaseTest {
  @Test
  public void testComputeMaxBatchSizeHash()
  {
    long expected = BatchSizePredictorImpl.computeMaxBatchSizeNoHash(
      100,
      25,
      100,
      2.0,
      4.0) +
      100 * IntVector.VALUE_WIDTH * 2;

    final long actual = BatchSizePredictorImpl.computeMaxBatchSize(
      100,
      25,
      100,
      2.0,
      4.0,
      true);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testComputeMaxBatchSizeNoHash() {
    final long expected = 1200;
    final long actual = BatchSizePredictorImpl.computeMaxBatchSize(
      100,
      25,
      100,
      2.0,
      1.5,
      false);
    final long actualNoHash = BatchSizePredictorImpl.computeMaxBatchSizeNoHash(
      100,
      25,
      100,
      2.0,
      1.5);

    Assert.assertEquals(expected, actual);
    Assert.assertEquals(expected, actualNoHash);
  }

  @Test
  public void testRoundUpPowerOf2() {
    long expected = 32;
    long actual = BatchSizePredictorImpl.roundUpToPowerOf2(expected);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testRounUpNonPowerOf2ToPowerOf2() {
    long expected = 32;
    long actual = BatchSizePredictorImpl.roundUpToPowerOf2(31);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testComputeValueVectorSizePowerOf2() {
    long expected = 4;
    long actual =
      BatchSizePredictorImpl.computeValueVectorSize(2, 2);

    Assert.assertEquals(expected, actual);
  }

  @Test
  public void testComputeValueVectorSizeNonPowerOf2() {
    long expected = 16;
    long actual =
      BatchSizePredictorImpl.computeValueVectorSize(3, 3);

    Assert.assertEquals(expected, actual);
  }
}
