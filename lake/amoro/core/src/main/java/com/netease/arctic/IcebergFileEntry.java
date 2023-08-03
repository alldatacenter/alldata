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

import com.netease.arctic.utils.ManifestEntryFields;
import org.apache.iceberg.ContentFile;
import org.apache.iceberg.relocated.com.google.common.base.MoreObjects;

/**
 * Entry of Iceberg ContentFile, include status, ContentFile, snapshotId and sequenceNumber.
 * If the actual sequenceNumber in manifest file is null, it will be inherited from metadata,
 * and it may be null for DELETED entries with iceberg version < 1.0.0.
 */
public class IcebergFileEntry {
  private final Long snapshotId;
  private final Long sequenceNumber;
  private final ManifestEntryFields.Status status;
  private final ContentFile<?> file;

  public IcebergFileEntry(Long snapshotId, Long sequenceNumber,
                          ManifestEntryFields.Status status, ContentFile<?> file) {
    this.snapshotId = snapshotId;
    this.sequenceNumber = sequenceNumber;
    this.status = status;
    this.file = file;
  }

  public Long getSnapshotId() {
    return snapshotId;
  }

  public Long getSequenceNumber() {
    return sequenceNumber;
  }

  public ContentFile<?> getFile() {
    return file;
  }

  public ManifestEntryFields.Status getStatus() {
    return status;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("snapshotId", snapshotId)
        .add("sequenceNumber", sequenceNumber)
        .add("status", status)
        .add("file", file == null ? "null" : file.path())
        .toString();
  }
}
