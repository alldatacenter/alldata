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
package org.apache.drill.exec.physical.impl.xsort;

import java.util.concurrent.TimeUnit;

import javax.inject.Named;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorAccessible;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;

import org.apache.drill.shaded.guava.com.google.common.base.Preconditions;
import org.apache.drill.shaded.guava.com.google.common.base.Stopwatch;

public abstract class SingleBatchSorterTemplate implements SingleBatchSorter, IndexedSortable{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SingleBatchSorterTemplate.class);

  private SelectionVector2 vector2;

  @Override
  public void setup(FragmentContext context, SelectionVector2 vector2, VectorAccessible incoming) throws SchemaChangeException{
    Preconditions.checkNotNull(vector2);
    this.vector2 = vector2;
    try {
      doSetup(context, incoming, null);
    } catch (IllegalStateException e) {
      throw new SchemaChangeException(e);
    }
  }

  @Override
  public void sort(SelectionVector2 vector2){
    QuickSort qs = new QuickSort();
    Stopwatch watch = Stopwatch.createStarted();
    if (vector2.getCount() > 0) {
      qs.sort(this, 0, vector2.getCount());
    }
    logger.debug("Took {} us to sort {} records", watch.elapsed(TimeUnit.MICROSECONDS), vector2.getCount());
  }

  @Override
  public void swap(int sv0, int sv1) {
    char tmp = vector2.getIndex(sv0);
    vector2.setIndex(sv0, vector2.getIndex(sv1));
    vector2.setIndex(sv1, tmp);
  }

  @Override
  public int compare(int leftIndex, int rightIndex) {
    char sv1 = vector2.getIndex(leftIndex);
    char sv2 = vector2.getIndex(rightIndex);
    try {
      return doEval(sv1, sv2);
    } catch (SchemaChangeException e) {
      throw new RuntimeException( e );
    }
  }

  public abstract void doSetup(@Named("context") FragmentContext context,
                               @Named("incoming") VectorAccessible incoming,
                               @Named("outgoing") RecordBatch outgoing)
                       throws SchemaChangeException;
  public abstract int doEval(@Named("leftIndex") char leftIndex,
                             @Named("rightIndex") char rightIndex)
                      throws SchemaChangeException;

  @Override
  public String toString() {
    return "SinglebatchSorterTemplate[vector2=" + vector2 + "]";
  }
}
