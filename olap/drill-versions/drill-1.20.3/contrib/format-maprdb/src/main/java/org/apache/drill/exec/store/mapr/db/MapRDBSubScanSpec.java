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
package org.apache.drill.exec.store.mapr.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mapr.db.index.IndexDesc;
import com.mapr.fs.jni.MapRConstants;
import com.mapr.org.apache.hadoop.hbase.util.Bytes;

public class MapRDBSubScanSpec implements Comparable<MapRDBSubScanSpec> {

  protected String tableName;
  protected IndexDesc indexDesc;
  protected String regionServer;
  protected byte[] startRow;
  protected byte[] stopRow;
  protected byte[] serializedFilter;
  protected String userName;

  @JsonCreator
  public MapRDBSubScanSpec(@JsonProperty("tableName") String tableName,
                           @JsonProperty("indexDesc") IndexDesc indexDesc,
                           @JsonProperty("regionServer") String regionServer,
                           @JsonProperty("startRow") byte[] startRow,
                           @JsonProperty("stopRow") byte[] stopRow,
                           @JsonProperty("serializedFilter") byte[] serializedFilter,
                           @JsonProperty("filterString") String filterString,
                           @JsonProperty("username") String userName) {
    if (serializedFilter != null && filterString != null) {
      throw new IllegalArgumentException("The parameters 'serializedFilter' or 'filterString' cannot be specified at the same time.");
    }
    this.tableName = tableName;
    this.indexDesc = indexDesc;
    this.regionServer = regionServer;
    this.startRow = startRow;
    this.stopRow = stopRow;
    this.serializedFilter = serializedFilter;
    this.userName = userName;
  }

  /* package */ MapRDBSubScanSpec() {
    // empty constructor, to be used with builder pattern;
  }

  public String getTableName() {
    return tableName;
  }

  public IndexDesc getIndexDesc() {
    return indexDesc;
  }

  public MapRDBSubScanSpec setTableName(String tableName) {
    this.tableName = tableName;
    return this;
  }

  public String getRegionServer() {
    return regionServer;
  }

  public MapRDBSubScanSpec setRegionServer(String regionServer) {
    this.regionServer = regionServer;
    return this;
  }

  /**
   * @return the raw (not-encoded) start row key for this sub-scan
   */
  public byte[] getStartRow() {
    return startRow == null ? MapRConstants.EMPTY_BYTE_ARRAY: startRow;
  }

  public MapRDBSubScanSpec setStartRow(byte[] startRow) {
    this.startRow = startRow;
    return this;
  }

  /**
   * @return the raw (not-encoded) stop row key for this sub-scan
   */
  public byte[] getStopRow() {
    return stopRow == null ? MapRConstants.EMPTY_BYTE_ARRAY : stopRow;
  }

  public MapRDBSubScanSpec setStopRow(byte[] stopRow) {
    this.stopRow = stopRow;
    return this;
  }

  public byte[] getSerializedFilter() {
    return serializedFilter;
  }

  public MapRDBSubScanSpec setSerializedFilter(byte[] serializedFilter) {
    this.serializedFilter = serializedFilter;
    return this;
  }

  public String getUserName() {
    return userName;
  }

  public void setUserName(String userName) {
    this.userName = userName;
  }

  @Override
  public String toString() {
    return "MapRDBSubScanSpec [tableName=" + tableName
        + ", startRow=" + (startRow == null ? null : Bytes.toStringBinary(startRow))
        + ", stopRow=" + (stopRow == null ? null : Bytes.toStringBinary(stopRow))
        + ", filter=" + (getSerializedFilter() == null ? null : Bytes.toBase64(getSerializedFilter()))
        + ", regionServer=" + regionServer
        + ", userName=" + userName + "]";
  }

  @Override
  /*
   * The semantics of the compareTo function is same as that of TabletInfoImpl.
   * It compares the startRows of the two subScanSpec and returns the status
   * if one is greater than the other. If the two startRows are same then it
   * compares the stopRows.
   */
  public int compareTo(MapRDBSubScanSpec o) {
    if (o == null) {
      return 1;
    } else {
      int result = Bytes.compareTo(this.getStartRow(), o.getStartRow());
      if (result != 0) {
        return result;
      } else {
        result = Bytes.compareTo(this.getStopRow(), o.getStopRow());
        if (result != 0) {
          if (this.getStartRow().length != 0 && this.getStopRow().length == 0) {
            return 1;
          } else {
            return o.getStartRow().length != 0 && o.getStopRow().length == 0 ? -1 : result;
          }
        } else {
          return 0;
        }
      }
    }
  }
}
