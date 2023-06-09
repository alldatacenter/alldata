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

package com.netease.arctic;

import org.apache.iceberg.ContentFile;

/**
 * Entry of Iceberg ContentFile, include ContentFile, snapshotId and sequenceNumber.
 * The sequenceNumber is inherited from metadata, not the actual value in manifest file.
 */
public class IcebergFileEntry {
  private Long snapshotId;
  private long sequenceNumber;
  private ContentFile<?> file;

  public IcebergFileEntry(Long snapshotId, long sequenceNumber, ContentFile<?> file) {
    this.snapshotId = snapshotId;
    this.sequenceNumber = sequenceNumber;
    this.file = file;
  }

  public Long getSnapshotId() {
    return snapshotId;
  }

  public void setSnapshotId(Long snapshotId) {
    this.snapshotId = snapshotId;
  }

  public long getSequenceNumber() {
    return sequenceNumber;
  }

  public void setSequenceNumber(long sequenceNumber) {
    this.sequenceNumber = sequenceNumber;
  }

  public ContentFile<?> getFile() {
    return file;
  }

  public void setFile(ContentFile<?> file) {
    this.file = file;
  }
  
}
