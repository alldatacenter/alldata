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
package org.apache.drill.exec.physical.impl.orderedpartitioner;

import javax.inject.Named;

import org.apache.drill.exec.exception.SchemaChangeException;
import org.apache.drill.exec.ops.FragmentContext;
import org.apache.drill.exec.record.RecordBatch;
import org.apache.drill.exec.record.VectorContainer;
import org.apache.drill.exec.record.selection.SelectionVector2;
import org.apache.hadoop.util.IndexedSortable;
import org.apache.hadoop.util.QuickSort;

public abstract class SampleSortTemplate implements SampleSorter, IndexedSortable{
  static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(SampleSortTemplate.class);

  SelectionVector2 vector2;

  public void setup(FragmentContext context, VectorContainer sampleBatch, SelectionVector2 vector2) throws SchemaChangeException{
    this.vector2 = vector2;
    doSetup(context, sampleBatch, null);
  }

  @Override
  public void sort(SelectionVector2 vector2, VectorContainer container){
    QuickSort qs = new QuickSort();
    qs.sort(this, 0, vector2.getCount());
  }

  @Override
  public void swap(int sv0, int sv1) {
    char tmp = vector2.getIndex(sv0);
    vector2.setIndex(sv0, vector2.getIndex(sv1));
    vector2.setIndex(sv1, tmp);
  }

  @Override
  public int compare(int leftIndex, int rightIndex) {
    return doEval(leftIndex, rightIndex);
  }

  public abstract void doSetup(@Named("context") FragmentContext context, @Named("incoming") VectorContainer incoming, @Named("outgoing") RecordBatch outgoing);
  public abstract int doEval(@Named("leftIndex") int leftIndex, @Named("rightIndex") int rightIndex);

}
