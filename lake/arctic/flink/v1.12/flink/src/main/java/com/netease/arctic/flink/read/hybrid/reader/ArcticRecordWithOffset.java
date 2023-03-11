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

package com.netease.arctic.flink.read.hybrid.reader;

/**
 * A record along with the reader position to be stored in the checkpoint.
 */
public class ArcticRecordWithOffset<T> {
  private T record;

  private int insertFileOffset;
  private long insertRecordOffset;
  private int deleteFileOffset;
  private long deleteRecordOffset;

  public T record() {
    return record;
  }

  public void record(T record) {
    this.record = record;
  }

  public int insertFileOffset() {
    return insertFileOffset;
  }

  public long insertRecordOffset() {
    return insertRecordOffset;
  }

  public int deleteFileOffset() {
    return deleteFileOffset;
  }

  public long deleteRecordOffset() {
    return deleteRecordOffset;
  }

  public void set(
      T newRecord,
      int insertFileOffset,
      long insertRecordOffset,
      int deleteFileOffset,
      long deleteRecordOffset) {
    this.record = newRecord;
    this.insertFileOffset = insertFileOffset;
    this.deleteFileOffset = deleteFileOffset;
    this.insertRecordOffset = insertRecordOffset;
    this.deleteRecordOffset = deleteRecordOffset;
  }
}
