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

import com.netease.arctic.data.DataTreeNode;
import com.netease.arctic.io.ArcticFileIO;
import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.PartitionSpec;
import org.apache.iceberg.StructLike;
import org.apache.iceberg.deletes.PositionDelete;
import org.apache.iceberg.encryption.EncryptionManager;
import org.apache.iceberg.io.DeleteWriteResult;
import org.apache.iceberg.io.FileAppenderFactory;
import org.apache.iceberg.io.FileWriter;
import org.apache.iceberg.relocated.com.google.common.collect.Maps;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Positional delete file writer for iceberg tables. Write to different delete file for every data file.
 * The output delete files are named with pattern: {data_file_name}-delete-{delete_file_suffix}.
 * 
 * @param <T> to indicate the record data type.
 */
public class ArcticTreeNodePosDeleteWriter<T> implements FileWriter<PositionDelete<T>, DeleteWriteResult>, SetTreeNode {

  private final Map<DataTreeNode, SortedPosDeleteWriter<T>> posDeletes = Maps.newHashMap();

  private SortedPosDeleteWriter<T> currentDeleteWriter;

  private DataTreeNode currentTreeNode;

  private final FileAppenderFactory<T> appenderFactory;
  private final FileFormat format;
  private final StructLike partition;
  private final ArcticFileIO fileIO;
  private final EncryptionManager encryptionManager;
  private Long transactionId;

  private String location;

  private PartitionSpec spec;


  public ArcticTreeNodePosDeleteWriter(
      FileAppenderFactory<T> appenderFactory,
      FileFormat format,
      StructLike partition,
      ArcticFileIO fileIO,
      EncryptionManager encryptionManager,
      Long transactionId,
      String location,
      PartitionSpec spec) {
    this.appenderFactory = appenderFactory;
    this.format = format;
    this.partition = partition;
    this.fileIO = fileIO;
    this.encryptionManager = encryptionManager;
    this.transactionId = transactionId;
    this.location = location;
    this.spec = spec;
  }

  @Override
  public long length() {
    throw new UnsupportedOperationException(
        this.getClass().getName() + " does not implement length");
  }

  @Override
  public void write(PositionDelete<T> payload) {
    delete(payload.path(), payload.pos());
  }

  public void delete(CharSequence path, long pos) {
    if (currentDeleteWriter == null) {
      throw new IllegalStateException("Please set tree node first");
    }
    currentDeleteWriter.delete(path, pos);
  }

  private SortedPosDeleteWriter<T> generatePosDelete(DataTreeNode treeNode) {
    return new SortedPosDeleteWriter<>(appenderFactory,
        new CommonOutputFileFactory(location, spec, format, fileIO,
            encryptionManager, 0, 0, transactionId), fileIO,
        format, treeNode.mask(), treeNode.index(), partition);
  }

  public List<DeleteFile> complete() throws IOException {
    List<DeleteFile> list = new ArrayList<>();
    for (SortedPosDeleteWriter<T> sortedPosDeleteWriter: posDeletes.values()) {
      list.addAll(sortedPosDeleteWriter.complete());
    }
    return list;
  }

  @Override
  public void close() throws IOException {
    for (SortedPosDeleteWriter<T> sortedPosDeleteWriter: posDeletes.values()) {
      sortedPosDeleteWriter.close();
    }
  }

  @Override
  public DeleteWriteResult result() {
    try {
      return new DeleteWriteResult(complete());
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void setTreeNode(DataTreeNode treeNode) {
    if (currentTreeNode != null && currentTreeNode.equals(treeNode)) return;

    currentDeleteWriter = posDeletes.computeIfAbsent(treeNode,
        this::generatePosDelete);
    this.currentTreeNode = treeNode;
  }

  @Override
  public DataTreeNode geTreeNode() {
    return currentTreeNode;
  }
}
