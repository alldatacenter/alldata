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

package org.apache.uniffle.common;

import java.util.Objects;

/**
 * Class for partition range: [start, end]
 * Note: both inclusive
 */
public class PartitionRange implements Comparable<PartitionRange> {

  private final int start;
  private final int end;

  public PartitionRange(int start, int end) {
    if (start > end || start < 0) {
      throw new IllegalArgumentException("Illegal partition range [start, end]");
    }
    this.start = start;
    this.end = end;
  }

  public int getStart() {
    return start;
  }

  public int getEnd() {
    return end;
  }

  public int getPartitionNum() {
    return end - start + 1;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PartitionRange that = (PartitionRange) o;
    return start == that.start && end == that.end;
  }

  @Override
  public int hashCode() {
    return Objects.hash(start, end);
  }

  @Override
  public String toString() {
    return "PartitionRange[" + start + ", " + end + ']';
  }

  @Override
  public int compareTo(PartitionRange o) {
    return this.getStart() - o.getStart();
  }
}
