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

package com.netease.arctic.io.writer;

import com.netease.arctic.data.DataFileType;
import com.netease.arctic.data.DataTreeNode;
import org.apache.iceberg.StructLike;

import java.util.Objects;

/**
 * Key of task writers, record with the same key can be written in the same file.
 */
public class TaskWriterKey {
  private final StructLike partitionKey;
  private final DataTreeNode treeNode;
  private final DataFileType fileType;

  public TaskWriterKey(StructLike partitionKey, DataTreeNode treeNode, DataFileType fileType) {
    this.partitionKey = partitionKey;
    this.treeNode = treeNode;
    this.fileType = fileType;
  }

  public StructLike getPartitionKey() {
    return partitionKey;
  }

  public DataTreeNode getTreeNode() {
    return treeNode;
  }

  public DataFileType getFileType() {
    return fileType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    TaskWriterKey writerKey = (TaskWriterKey) o;
    return Objects.equals(partitionKey, writerKey.partitionKey) &&
        Objects.equals(treeNode, writerKey.treeNode) &&
        fileType == writerKey.fileType;
  }

  @Override
  public int hashCode() {
    return Objects.hash(partitionKey, treeNode, fileType);
  }

}
