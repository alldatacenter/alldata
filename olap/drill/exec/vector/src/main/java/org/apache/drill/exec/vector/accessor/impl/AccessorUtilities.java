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

public class AccessorUtilities {

  private AccessorUtilities() { }

  public static int sv4Batch(int sv4Index) {
    return sv4Index >>> 16;
  }

  public static int sv4Index(int sv4Index) {
    return sv4Index & 0xFFFF;
  }

  public static String bytesToString(byte[] value) {
    StringBuilder buf = new StringBuilder()
        .append("[");
    int len = Math.min(value.length, 20);
    for (int i = 0; i < len;  i++) {
      if (i > 0) {
        buf.append(", ");
      }
      String str = Integer.toHexString(value[i] & 0xFF);
      if (str.length() < 2) {
        buf.append("0");
      }
      buf.append(str);
    }
    if (value.length > len) {
      buf.append("...");
    }
    buf.append("]");
    return buf.toString();
  }

}
