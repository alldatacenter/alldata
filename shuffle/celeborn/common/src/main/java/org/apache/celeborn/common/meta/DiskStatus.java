/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.meta;

public enum DiskStatus {
  HEALTHY(0),
  READ_OR_WRITE_FAILURE(1),
  IO_HANG(2),
  HIGH_DISK_USAGE(3),
  CRITICAL_ERROR(4);

  private final byte value;

  DiskStatus(int value) {
    assert (value >= 0 && value < 256);
    this.value = (byte) value;
  }

  public final byte getValue() {
    return value;
  }

  public final String toMetric() {
    String[] fragments = this.name().split("_");
    String metric = "";
    for (String fragment : fragments) {
      int len = fragment.length();
      if (len >= 1) {
        metric += fragment.substring(0, 1).toUpperCase();
        metric += fragment.substring(1, len).toLowerCase();
      }
    }
    return metric;
  }
}
