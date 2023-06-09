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

import org.apache.drill.exec.vector.AllocationHelper;
import org.apache.drill.exec.vector.ValueVector;

/**
 * VectorAccessible is an interface. Yet, several operations are done
 * on VectorAccessible over and over gain. While Java 8 allows static
 * methods on an interface, Drill uses Java 7, which does not. This
 * class is a placeholder for common VectorAccessible methods that
 * can migrate into the interface when Drill upgrades to Java 8.
 */

public class VectorAccessibleUtilities {

  private VectorAccessibleUtilities() { }

  public static void clear(VectorAccessible va) {
    for (final VectorWrapper<?> w : va) {
      w.clear();
    }
  }

  public static void clear(Iterable<ValueVector> iter) {
    for (final ValueVector v : iter) {
      v.clear();
    }
  }

  public static void setValueCount(VectorAccessible va, int count) {
    for (VectorWrapper<?> w: va) {
      w.getValueVector().getMutator().setValueCount(count);
    }
  }

  public static void allocateVectors(VectorAccessible va, int targetRecordCount) {
    for (VectorWrapper<?> w: va) {
      AllocationHelper.allocateNew(w.getValueVector(), targetRecordCount);
    }
  }
}
