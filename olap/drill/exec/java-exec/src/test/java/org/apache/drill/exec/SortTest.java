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
package org.apache.drill.exec;

import java.util.Random;

import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;

public class SortTest {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SortTest.class);

  private static final int RECORD_COUNT = 10*1000*1000;
  private static final int KEY_SIZE = 10;
  private static final int DATA_SIZE = 90;
  private static final int RECORD_SIZE = KEY_SIZE + DATA_SIZE;

  private byte[] data;

  public static void main(String[] args) throws Exception{
    for(int i =0; i < 100; i++){
      SortTest st = new SortTest();
      long nanos = st.doSort();
      logger.info("Sort Completed in {} ns.", nanos);
    }
  }

  SortTest(){
    logger.info("Generating data... ");
    data = new byte[RECORD_SIZE*RECORD_COUNT];
    Random r = new Random();
    r.nextBytes(data);
    logger.info("Data generated. ");
  }

  public long doSort(){
    QuickSort qs = new QuickSort();
    ByteSortable b = new ByteSortable();
    long nano = System.nanoTime();
    qs.sort(b, 0, RECORD_COUNT);
    return System.nanoTime() - nano;
  }

  private class ByteSortable implements IndexedSortable{
    final byte[] space = new byte[RECORD_SIZE];
    final BytesWritable.Comparator comparator = new BytesWritable.Comparator();

    @Override
    public int compare(int index1, int index2) {
      return comparator.compare(data, index1*RECORD_SIZE, KEY_SIZE, data, index2*RECORD_SIZE, KEY_SIZE);
    }

    @Override
    public void swap(int index1, int index2) {
      int start1 = index1*RECORD_SIZE;
      int start2 = index2*RECORD_SIZE;
      System.arraycopy(data, start1, space, 0, RECORD_SIZE);
      System.arraycopy(data, start2, data, start1, RECORD_SIZE);
      System.arraycopy(space, 0, data, start2, RECORD_SIZE);
    }
  }
}
