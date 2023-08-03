/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.data;

import org.apache.iceberg.relocated.com.google.common.base.Preconditions;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;


/**
 * Tree node which data shuffle into, consist of two parts.
 * <ul>
 *   <li>mask: mask code representing the height of tree, validate value:2^n-1(n representing the height of tree)</li>
 *   <li>index: index of tree node(starting from 0)</li>
 * </ul>
 * index = hashCode(data) & mask;
 */
public final class DataTreeNode implements Serializable {

  private final long mask;
  private final long index;

  public static final DataTreeNode ROOT = new DataTreeNode(0, 0);

  public static DataTreeNode of(long mask, long index) {
    if (index > mask) {
      throw new IllegalArgumentException("index can not be greater than mask");
    }
    return new DataTreeNode(mask, index);
  }

  public static DataTreeNode ofId(long id) {
    long mask = maskOfId(id);
    long index = id - mask - 1;
    return new DataTreeNode(mask, index);
  }

  public static long maskOfId(long id) {
    long temp = id;
    int i = 0;
    while (temp != 1) {
      temp = temp >> 1;
      i++;
    }

    return (1L << i) - 1;
  }

  private DataTreeNode(long mask, long index) {
    this.mask = mask;
    this.index = index;
  }

  public long mask() {
    return mask;
  }

  public long index() {
    return index;
  }

  public boolean isSonOf(@Nonnull DataTreeNode another) {
    if (another.equals(this)) {
      return true;
    }
    if (this.mask <= another.mask()) {
      return false;
    }
    return (this.index & another.mask) == another.index;
  }

  public DataTreeNode left() {
    return DataTreeNode.of((this.mask << 1) + 1, this.index);
  }

  public DataTreeNode right() {
    return DataTreeNode.of((this.mask << 1) + 1, this.index + this.mask + 1);
  }

  public DataTreeNode parent() {
    Preconditions.checkArgument(mask > 0);
    long parentMask = mask - 1 >> 1;
    long parentIndex;
    if (index > parentMask) {
      parentIndex = index - parentMask - 1;
    } else {
      parentIndex = index;
    }
    return DataTreeNode.of(parentMask, parentIndex);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    DataTreeNode treeNode = (DataTreeNode) o;
    return mask == treeNode.mask &&
        index == treeNode.index;
  }

  @Override
  public int hashCode() {
    return Objects.hash(mask, index);
  }

  @Override
  public String toString() {
    return String.format("TreeNode(%s,%s)", mask, index);
  }

  public long getMask() {
    return mask;
  }

  public long getIndex() {
    return index;
  }

  public long getId() {
    return mask + 1 + index;
  }
}
