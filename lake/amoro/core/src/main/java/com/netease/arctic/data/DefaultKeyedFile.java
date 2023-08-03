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

import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.StructLike;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Default implementation of {@link PrimaryKeyedFile}, wrapping a {@link DataFile} and parsing extra information from
 * file name.
 */
public class DefaultKeyedFile implements PrimaryKeyedFile, Serializable {

  private final DataFile internalFile;

  private final FileMeta meta;

  private DefaultKeyedFile(DataFile internalFile, FileMeta meta) {
    this.internalFile = internalFile;
    this.meta = meta;
  }

  public static DefaultKeyedFile parseChange(DataFile dataFile, long sequenceNumber) {
    FileMeta fileMeta = FileNameRules.parseChange(dataFile.path().toString(), sequenceNumber);
    return new DefaultKeyedFile(dataFile, fileMeta);
  }

  public static DefaultKeyedFile parseBase(DataFile dataFile) {
    FileMeta fileMeta = FileNameRules.parseBase(dataFile.path().toString());
    return new DefaultKeyedFile(dataFile, fileMeta);
  }

  @Override
  public Long transactionId() {
    return meta.transactionId();
  }

  @Override
  public DataFileType type() {
    return meta.type();
  }

  @Override
  public Long pos() {
    return internalFile.pos();
  }

  @Override
  public int specId() {
    return internalFile.specId();
  }

  @Override
  public CharSequence path() {
    return internalFile.path();
  }

  @Override
  public FileFormat format() {
    return internalFile.format();
  }

  @Override
  public long recordCount() {
    return internalFile.recordCount();
  }

  @Override
  public long fileSizeInBytes() {
    return internalFile.fileSizeInBytes();
  }

  @Override
  public Map<Integer, Long> columnSizes() {
    return internalFile.columnSizes();
  }

  @Override
  public Map<Integer, Long> valueCounts() {
    return internalFile.valueCounts();
  }

  @Override
  public Map<Integer, Long> nullValueCounts() {
    return internalFile.nullValueCounts();
  }

  @Override
  public Map<Integer, Long> nanValueCounts() {
    return internalFile.nanValueCounts();
  }

  @Override
  public Map<Integer, ByteBuffer> lowerBounds() {
    return internalFile.lowerBounds();
  }

  @Override
  public Map<Integer, ByteBuffer> upperBounds() {
    return internalFile.upperBounds();
  }

  @Override
  public ByteBuffer keyMetadata() {
    return internalFile.keyMetadata();
  }

  @Override
  public List<Long> splitOffsets() {
    return internalFile.splitOffsets();
  }

  @Override
  public DataFile copy() {
    return new DefaultKeyedFile(internalFile.copy(), meta);
  }

  @Override
  public DataFile copyWithoutStats() {
    return new DefaultKeyedFile(internalFile.copyWithoutStats(), meta);
  }

  @Override
  public StructLike partition() {
    return internalFile.partition();
  }

  @Override
  public DataTreeNode node() {
    return meta.node();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DefaultKeyedFile that = (DefaultKeyedFile) o;
    return Objects.equals(internalFile.path(), that.internalFile.path());
  }

  @Override
  public int hashCode() {
    return Objects.hash(internalFile.path());
  }

  public static class FileMeta implements Serializable {
    private final long transactionId;
    private final DataFileType type;
    private final DataTreeNode node;

    public FileMeta(long transactionId, DataFileType type, DataTreeNode node) {
      this.transactionId = transactionId;
      this.type = type;
      this.node = node;
    }

    public long transactionId() {
      return transactionId;
    }

    public DataFileType type() {
      return type;
    }

    public DataTreeNode node() {
      return node;
    }
  }
}
