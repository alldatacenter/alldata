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
package org.apache.drill.exec.store.iceberg;

import org.apache.drill.exec.store.schedule.CompleteWork;
import org.apache.drill.exec.store.schedule.EndpointByteMap;
import org.apache.iceberg.CombinedScanTask;
import org.apache.iceberg.FileScanTask;

public class IcebergCompleteWork implements CompleteWork {
  private final EndpointByteMap byteMap;

  private final CombinedScanTask scanTask;

  private final long totalBytes;

  public IcebergCompleteWork(
    EndpointByteMap byteMap,
    CombinedScanTask scanTask) {
    this.byteMap = byteMap;
    this.scanTask = scanTask;
    this.totalBytes = scanTask.files().stream()
      .mapToLong(FileScanTask::length)
      .sum();
  }

  public CombinedScanTask getScanTask() {
    return scanTask;
  }

  @Override
  public long getTotalBytes() {
    return totalBytes;
  }

  @Override
  public EndpointByteMap getByteMap() {
    return byteMap;
  }

  @Override
  public int compareTo(CompleteWork o) {
    return 0;
  }
}
