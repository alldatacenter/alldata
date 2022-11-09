/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.flink.core.typeutils.base;

import com.bytedance.bitsail.flink.core.typeutils.base.view.TestInputView;
import com.bytedance.bitsail.flink.core.typeutils.base.view.TestOutputView;

import org.apache.flink.api.common.operators.Order;
import org.apache.flink.api.common.typeutils.TypeComparator;
import org.apache.flink.api.common.typeutils.TypeSerializer;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public abstract class ColumnComparatorTestBase<T> {

  protected Order[] getTestedOrder() {
    return new Order[] {Order.ASCENDING, Order.DESCENDING};
  }

  @Test
  public void testInequality() {
    for (Order order : getTestedOrder()) {
      boolean ascending = isAscending(order);
      testGreatSmallAscDesc(ascending, true);
      testGreatSmallAscDesc(ascending, false);
    }
  }

  protected void testGreatSmallAscDesc(boolean ascending, boolean greater) {
    try {
      // split data into low and high part
      T[] data = getSortedData();

      TypeComparator<T> comparator = getComparator(ascending);
      TestOutputView out1;
      TestOutputView out2;
      TestInputView in1;
      TestInputView in2;

      // compares every element in high with every element in low
      for (int x = 0; x < data.length - 1; x++) {
        for (int y = x + 1; y < data.length; y++) {
          out1 = new TestOutputView();
          writeSortedData(data[x], out1);
          in1 = out1.getInputView();

          out2 = new TestOutputView();
          writeSortedData(data[y], out2);
          in2 = out2.getInputView();

          if (greater && ascending) {
            assertTrue(comparator.compareSerialized(in1, in2) < 0);
          }
          if (greater && !ascending) {
            assertTrue(comparator.compareSerialized(in1, in2) > 0);
          }
          if (!greater && ascending) {
            assertTrue(comparator.compareSerialized(in2, in1) > 0);
          }
          if (!greater && !ascending) {
            assertTrue(comparator.compareSerialized(in2, in1) < 0);
          }
        }
      }
    } catch (Exception e) {
      System.err.println(e.getMessage());
      e.printStackTrace();
      fail("Exception in test: " + e.getMessage());
    }
  }

  protected void writeSortedData(T value, TestOutputView out) throws IOException {
    TypeSerializer<T> serializer = getSerializer();

    // Write data into a outputView
    serializer.serialize(value, out);

    // This are the same tests like in the serializer
    // Just look if the data is really there after serialization, before testing comparator on
    // it
    TestInputView in = out.getInputView();
    assertTrue("No data available during deserialization.", in.available() > 0);

    T deserialized = serializer.deserialize(serializer.createInstance(), in);
  }

  protected TypeSerializer<T> getSerializer() {
    TypeSerializer<T> serializer = createSerializer();
    if (serializer == null) {
      throw new RuntimeException("Test case corrupt. Returns null as serializer.");
    }
    return serializer;
  }

  private static boolean isAscending(Order order) {
    return order == Order.ASCENDING;
  }

  public abstract TypeComparator<T> getComparator(boolean ascending);

  public abstract TypeSerializer<T> createSerializer();

  public abstract T[] getSortedData();
}
