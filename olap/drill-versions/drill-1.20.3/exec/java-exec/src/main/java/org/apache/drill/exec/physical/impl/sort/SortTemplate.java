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
package org.apache.drill.exec.physical.impl.sort;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector4;
import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

public abstract class SortTemplate implements Sorter, IndexedSortable{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SortTemplate.class);

  private SelectionVector4 vector4;


  public void setup(FragmentContext context, SelectionVector4 vector4, VectorContainer hyperBatch) throws SchemaChangeException{
    // we pass in the local hyperBatch since that is where we'll be reading data.
    Preconditions.checkNotNull(vector4);
    this.vector4 = vector4;
    doSetup(context, hyperBatch, null);
  }

  @Override
  public void sort(SelectionVector4 vector4, VectorContainer container){
    Stopwatch watch = Stopwatch.createStarted();
    QuickSort qs = new QuickSort();
    qs.sort(this, 0, vector4.getTotalCount());
    logger.debug("Took {} us to sort {} records", watch.elapsed(TimeUnit.MICROSECONDS), vector4.getTotalCount());
  }

  @Override
  public void swap(int sv0, int sv1) {
    int tmp = vector4.get(sv0);
    vector4.set(sv0, vector4.get(sv1));
    vector4.set(sv1, tmp);
  }

  @Override
  public int compare(int leftIndex, int rightIndex) {
    int sv1 = vector4.get(leftIndex);
    int sv2 = vector4.get(rightIndex);
    return doEval(sv1, sv2);
  }

  public abstract void doSetup(@Named("context") FragmentContext context, @Named("incoming") VectorContainer incoming, @Named("outgoing") RecordBatch outgoing);
  public abstract int doEval(@Named("leftIndex") int leftIndex, @Named("rightIndex") int rightIndex);

  @Override
  public String toString() {
    return "SortTemplate[vector4=" + vector4 + "]";
  }
}
