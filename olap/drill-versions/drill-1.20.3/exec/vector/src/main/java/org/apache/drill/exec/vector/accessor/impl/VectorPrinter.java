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
package org.apache.drill.exec.vector.accessor.impl;

import org.apache.drill.exec.vector.UInt4Vector;
import org.apache.drill.exec.vector.ValueVector;
import org.apache.drill.exec.vector.VarCharVector;
import org.apache.drill.shaded.guava.com.google.common.base.Charsets;

/**
 * Handy tool to visualize string and offset vectors for
 * debugging.
 */

public class VectorPrinter {

  public static void printOffsets(UInt4Vector vector, int maxPrint) {
    int capacity = vector.getAllocatedSize() / 4;
    if (capacity == 0) {
      return;
    }
    int length = Math.max(maxPrint, vector.getAccessor().getValueCount());
    printOffsets(vector, 0, length);
  }

  public static void printOffsets(UInt4Vector vector, int start, int length) {
    header(vector, start, length);
    for (int i = start, j = 0; j < length; i++, j++) {
      if (j % 40 == 0) {
        System.out.print("\n          ");
      }
      else if (j > 0) {
        System.out.print(" ");
      }
      System.out.print(vector.getAccessor().get(i));
    }
    System.out.print("], addr = ");
    System.out.println(vector.getBuffer().addr());
  }

  public static void printStrings(VarCharVector vector, int start, int length) {
    printOffsets(vector.getOffsetVector(), start, length + 1);
    header(vector, start, length);
    System.out.println();
    for (int i = start, j = 0; j < length; i++, j++) {
      System.out.print("  ");
      System.out.print(i);
      System.out.print(": \"");
      System.out.print(stringAt(vector, i));
      System.out.println("\"");
    }
    System.out.println("]");
  }

  public static void header(ValueVector vector, int start, int length) {
    System.out.print(vector.getClass());
    System.out.print(": (");
    System.out.print(start);
    System.out.print(" - ");
    System.out.print(start + length - 1);
    System.out.print("): [");
  }

  public static String stringAt(VarCharVector vector, int i) {
    return new String(vector.getAccessor().get(i), Charsets.UTF_8);
  }
}
