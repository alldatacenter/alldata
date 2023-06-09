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
package org.apache.drill.exec.record;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.complex.AbstractMapVector;
import org.apache.drill.exec.vector.complex.AbstractRepeatedMapVector;
import org.apache.drill.common.map.CaseInsensitiveMap;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;

/**
 * Prototype mechanism to allocate vectors based on expected
 * data sizes. This version uses a name-based map of fields
 * to sizes. Better to represent the batch structurally and
 * simply iterate over the schema rather than doing a per-field
 * lookup. But, the mechanisms needed to do the efficient solution
 * don't exist yet.
 * <p>
 * The element count is a float because it might be fractional.
 * If every tenth array has an element, then the count per array
 * is 0.1.
 */

public class VectorInitializer {

  @VisibleForTesting
  public static class AllocationHint {
    public final int entryWidth;
    public final float elementCount;

    private AllocationHint(int width, float elements) {
      entryWidth = width;
      elementCount = elements;
    }

    @Override
    public String toString() {
      StringBuilder buf = new StringBuilder()
        .append("{");
      String sep = "";
      if (entryWidth > 0) {
        buf.append("width=")
          .append(entryWidth);
        sep = ", ";
      }
      if (elementCount > 0) {
        buf.append(sep)
          .append("elements=")
          .append(elementCount);
      }
      buf.append("}");
      return buf.toString();
    }
  }

  private Map<String, AllocationHint> hints = CaseInsensitiveMap.newHashMap();

  public void variableWidth(String name, int width) {
    hints.put(name, new AllocationHint(width, 1));
  }

  public void fixedWidthArray(String name, float elements) {
    hints.put(name, new AllocationHint(0, elements));
  }

  public void variableWidthArray(String name, float width, float elements) {
    hints.put(name, new AllocationHint((int) Math.ceil(width), elements));
  }

  public AllocationHint hint(String name) {
    return hints.get(name);
  }

  public void allocateBatch(VectorAccessible va, int recordCount) {
    for (VectorWrapper<?> w: va) {
      allocateVector(w.getValueVector(), "", recordCount);
    }
  }

  public void allocateVectors(List<ValueVector> valueVectors, int recordCount) {
    for (ValueVector v : valueVectors) {
      allocateVector(v, v.getField().getName(), recordCount);
    }
  }

  public void allocateVector(ValueVector vector, String prefix, int recordCount) {
    String key = prefix + vector.getField().getName();
    AllocationHint hint = hints.get(key);
    if (vector instanceof AbstractMapVector) {
      allocateMap((AbstractMapVector) vector, prefix, recordCount, hint);
    } else {
      allocateVector(vector, recordCount, hint);
    }
//    Set<BufferLedger> ledgers = new HashSet<>();
//    vector.collectLedgers(ledgers);
//    int size = 0;
//    for (BufferLedger ledger : ledgers) {
//      size += ledger.getAccountedSize();
//    }
//    System.out.println(key + ": " + vector.getField().toString() +
//        " " +
//        ((hint == null) ? "no hint" : hint.toString()) +
//        ", " + size);
  }

  public void allocateVector(ValueVector vector, int recordCount, AllocationHint hint) {
    if (hint == null) {
      // Use hard-coded values. Same as ScanBatch

      AllocationHelper.allocate(vector, recordCount, 50, 10);
    } else {
      AllocationHelper.allocate(vector, recordCount, hint.entryWidth, hint.elementCount);
    }
  }

  private void allocateMap(AbstractMapVector map, String prefix, int recordCount, AllocationHint hint) {
    if (map instanceof AbstractRepeatedMapVector) {
      ((AbstractRepeatedMapVector) map).allocateOffsetsNew(recordCount);
      if (hint == null) {
        recordCount *= 10;
      } else {
        recordCount *= Math.round(hint.elementCount);
      }
    }
    prefix += map.getField().getName() + ".";
    for (ValueVector vector : map) {
      allocateVector(vector, prefix, recordCount);
    }
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("[" + getClass().getSimpleName())
       .append(" ");
    boolean first = true;
    for (Entry<String, AllocationHint>entry : hints.entrySet()) {
      if (! first) {
        buf.append(", ");
      }
      first = false;
      buf.append("[")
         .append(entry.getKey())
         .append(" ")
         .append(entry.getValue().toString())
         .append("]");
    }
    buf.append("]");
    return buf.toString();
  }
}
