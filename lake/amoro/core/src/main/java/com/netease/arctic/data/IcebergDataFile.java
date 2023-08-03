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

package com.netease.arctic.data;

import org.apache.iceberg.DataFile;
import org.apache.iceberg.FileFormat;
import org.apache.iceberg.StructLike;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;

public class IcebergDataFile extends IcebergContentFile<DataFile> implements DataFile {

  private static final long serialVersionUID = 1L;

  private final DataFile dataFile;

  public IcebergDataFile(DataFile dataFile, long sequenceNumber) {
    super(sequenceNumber);
    this.dataFile = dataFile;
  }

  @Override
  public DataFile internalFile() {
    return dataFile;
  }

  @Override
  public Long pos() {
    return dataFile.pos();
  }

  @Override
  public int specId() {
    return dataFile.specId();
  }

  @Override
  public CharSequence path() {
    return dataFile.path();
  }

  @Override
  public FileFormat format() {
    return dataFile.format();
  }

  @Override
  public StructLike partition() {
    return dataFile.partition();
  }

  @Override
  public long recordCount() {
    return dataFile.recordCount();
  }

  @Override
  public long fileSizeInBytes() {
    return dataFile.fileSizeInBytes();
  }

  @Override
  public Map<Integer, Long> columnSizes() {
    return dataFile.columnSizes();
  }

  @Override
  public Map<Integer, Long> valueCounts() {
    return dataFile.valueCounts();
  }

  @Override
  public Map<Integer, Long> nullValueCounts() {
    return dataFile.nullValueCounts();
  }

  @Override
  public Map<Integer, Long> nanValueCounts() {
    return dataFile.nanValueCounts();
  }

  @Override
  public Map<Integer, ByteBuffer> lowerBounds() {
    return dataFile.lowerBounds();
  }

  @Override
  public Map<Integer, ByteBuffer> upperBounds() {
    return dataFile.upperBounds();
  }

  @Override
  public ByteBuffer keyMetadata() {
    return dataFile.keyMetadata();
  }

  @Override
  public List<Long> splitOffsets() {
    return dataFile.splitOffsets();
  }

  @Override
  public DataFile copy() {
    return new IcebergDataFile(dataFile.copy(), getSequenceNumber());
  }

  @Override
  public DataFile copyWithoutStats() {
    return new IcebergDataFile(dataFile.copyWithoutStats(), getSequenceNumber());
  }

  public DataFile internalDataFile() {
    return dataFile;
  }
}
