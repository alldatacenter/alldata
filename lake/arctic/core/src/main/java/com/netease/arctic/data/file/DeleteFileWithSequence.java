/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.netease.arctic.data.file;

import org.apache.iceberg.DeleteFile;
import org.apache.iceberg.FileContent;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.StructLike;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class DeleteFileWithSequence implements DeleteFile, ContentFileWithSequence<DeleteFile>, Serializable {

  private static final long serialVersionUID = 1L;

  private DeleteFile deleteFile;

  private long sequenceNumber;

  public DeleteFileWithSequence(DeleteFile deleteFile, long sequenceNumber) {
    this.deleteFile = deleteFile;
    this.sequenceNumber = sequenceNumber;
  }

  @Override
  public long getSequenceNumber() {
    return sequenceNumber;
  }

  @Override
  public List<Long> splitOffsets() {
    return deleteFile.splitOffsets();
  }

  @Override
  public Long pos() {
    return deleteFile.pos();
  }

  @Override
  public int specId() {
    return deleteFile.specId();
  }

  @Override
  public FileContent content() {
    return deleteFile.content();
  }

  @Override
  public CharSequence path() {
    return deleteFile.path();
  }

  @Override
  public FileFormat format() {
    return deleteFile.format();
  }

  @Override
  public StructLike partition() {
    return deleteFile.partition();
  }

  @Override
  public long recordCount() {
    return deleteFile.recordCount();
  }

  @Override
  public long fileSizeInBytes() {
    return deleteFile.fileSizeInBytes();
  }

  @Override
  public Map<Integer, Long> columnSizes() {
    return deleteFile.columnSizes();
  }

  @Override
  public Map<Integer, Long> valueCounts() {
    return deleteFile.valueCounts();
  }

  @Override
  public Map<Integer, Long> nullValueCounts() {
    return deleteFile.nullValueCounts();
  }

  @Override
  public Map<Integer, Long> nanValueCounts() {
    return deleteFile.nanValueCounts();
  }

  @Override
  public Map<Integer, ByteBuffer> lowerBounds() {
    return deleteFile.lowerBounds();
  }

  @Override
  public Map<Integer, ByteBuffer> upperBounds() {
    return deleteFile.upperBounds();
  }

  @Override
  public ByteBuffer keyMetadata() {
    return deleteFile.keyMetadata();
  }

  @Override
  public List<Integer> equalityFieldIds() {
    return deleteFile.equalityFieldIds();
  }

  @Override
  public Integer sortOrderId() {
    return deleteFile.sortOrderId();
  }

  @Override
  public DeleteFile copy() {
    return new DeleteFileWithSequence(deleteFile.copy(), sequenceNumber);
  }

  @Override
  public DeleteFile copyWithoutStats() {
    return new DeleteFileWithSequence(deleteFile.copyWithoutStats(), sequenceNumber);
  }
}
