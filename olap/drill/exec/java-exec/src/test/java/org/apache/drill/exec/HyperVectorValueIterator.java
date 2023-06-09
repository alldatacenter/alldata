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

import java.util.Iterator;

import org.apache.drill.categories.VectorTest;
import org.apache.drill.exec.record.HyperVectorWrapper;
import org.apache.drill.exec.record.MaterializedField;
import org.apache.drill.exec.vector.ValueVector;
import org.junit.experimental.categories.Category;

@Category(VectorTest.class)
public class HyperVectorValueIterator implements Iterator<Object> {
  private MaterializedField mf;
  private HyperVectorWrapper<?> hyperVector;
  private int indexInVectorList;
  private int indexInCurrentVector;
  private ValueVector currVec;
  private long totalValues;
  private long totalValuesRead;
  // limit how many values will be read out of this iterator
  private long recordLimit;

  public HyperVectorValueIterator(MaterializedField mf, HyperVectorWrapper<?> hyperVector) {
    this.mf = mf;
    this.hyperVector = hyperVector;
    this.totalValues = 0;
    this.indexInCurrentVector = 0;
    this.indexInVectorList = 0;
    this.recordLimit = -1;
  }

  public void setRecordLimit(long limit) {
    this.recordLimit = limit;
  }

  public HyperVectorWrapper<?> getHyperVector() {
    return hyperVector;
  }

  public long getTotalRecords() {
    if (recordLimit > 0) {
      return recordLimit;
    } else {
      return totalValues;
    }
  }

  public void determineTotalSize() {
    for (ValueVector vv : hyperVector.getValueVectors()) {
      this.totalValues += vv.getAccessor().getValueCount();
    }
  }

  @Override
  public boolean hasNext() {
    if (totalValuesRead == recordLimit) {
      return false;
    }
    if (indexInVectorList < hyperVector.getValueVectors().length) {
      return true;
    } else if ( indexInCurrentVector < currVec.getAccessor().getValueCount()) {
      return true;
    }
    return false;
  }

  @Override
  public Object next() {
    if (currVec == null || indexInCurrentVector == currVec.getAccessor().getValueCount()) {
      currVec = hyperVector.getValueVectors()[indexInVectorList];
      indexInVectorList++;
      indexInCurrentVector = 0;
    }
    Object obj = currVec.getAccessor().getObject(indexInCurrentVector);
    indexInCurrentVector++;
    totalValuesRead++;
    return obj;
  }

  @Override
  public void remove() {
    throw new UnsupportedOperationException();
  }
}
