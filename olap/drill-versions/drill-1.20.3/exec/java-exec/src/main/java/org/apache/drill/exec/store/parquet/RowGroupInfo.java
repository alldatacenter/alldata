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
package org.apache.drill.exec.store.parquet;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.drill.exec.store.dfs.ReadEntryFromHDFS;
import org.apache.drill.exec.store.dfs.easy.FileWork;
import org.apache.drill.exec.store.schedule.CompleteWork;
import org.apache.drill.exec.store.schedule.EndpointByteMap;
import org.apache.hadoop.fs.Path;

import java.util.List;

import static org.apache.drill.exec.store.parquet.metadata.MetadataBase.ColumnMetadata;

public class RowGroupInfo extends ReadEntryFromHDFS implements CompleteWork, FileWork {

  private EndpointByteMap byteMap;
  private int rowGroupIndex;
  private List<? extends ColumnMetadata> columns;
  private long rowCount;  // rowCount = -1 indicates to include all rows.
  private long numRecordsToRead;

  @JsonCreator
  public RowGroupInfo(@JsonProperty("path") Path path,
                      @JsonProperty("start") long start,
                      @JsonProperty("length") long length,
                      @JsonProperty("rowGroupIndex") int rowGroupIndex,
                      long rowCount) {
    super(path, start, length);
    this.rowGroupIndex = rowGroupIndex;
    this.rowCount = rowCount;
    this.numRecordsToRead = rowCount;
  }

  public RowGroupReadEntry getRowGroupReadEntry() {
    return new RowGroupReadEntry(this.getPath(), this.getStart(), this.getLength(), this.rowGroupIndex, this.getNumRecordsToRead());
  }

  public int getRowGroupIndex() {
    return this.rowGroupIndex;
  }

  @Override
  public int compareTo(CompleteWork o) {
    return Long.compare(getTotalBytes(), o.getTotalBytes());
  }

  @Override
  public long getTotalBytes() {
    return this.getLength();
  }

  @Override
  public EndpointByteMap getByteMap() {
    return byteMap;
  }

  public long getNumRecordsToRead() {
    return numRecordsToRead;
  }

  public void setNumRecordsToRead(long numRecords) {
    numRecordsToRead = numRecords;
  }

  public void setEndpointByteMap(EndpointByteMap byteMap) {
    this.byteMap = byteMap;
  }

  public long getRowCount() {
    return rowCount;
  }

  public List<? extends ColumnMetadata> getColumns() {
    return columns;
  }

  public void setColumns(List<? extends ColumnMetadata> columns) {
    this.columns = columns;
  }
}
