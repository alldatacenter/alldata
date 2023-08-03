/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.celeborn.common.write;

import java.util.ArrayList;

import org.apache.celeborn.common.protocol.PartitionLocation;

public class DataBatches {
  private int totalSize = 0;
  private ArrayList<DataBatch> batches = new ArrayList<>();

  public static class DataBatch {
    public final PartitionLocation loc;
    public final int batchId;
    public final byte[] body;

    public DataBatch(PartitionLocation loc, int batchId, byte[] body) {
      this.loc = loc;
      this.batchId = batchId;
      this.body = body;
    }
  }

  public synchronized void addDataBatch(PartitionLocation loc, int batchId, byte[] body) {
    DataBatch dataBatch = new DataBatch(loc, batchId, body);
    batches.add(dataBatch);
    totalSize += body.length;
  }

  public int getTotalSize() {
    return totalSize;
  }

  public ArrayList<DataBatch> requireBatches() {
    totalSize = 0;
    ArrayList<DataBatch> allBatches = batches;
    batches = null;
    return allBatches;
  }

  public ArrayList<DataBatch> requireBatches(int requestSize) {
    if (requestSize >= totalSize) {
      totalSize = 0;
      return batches;
    }
    ArrayList<DataBatch> retBatches = new ArrayList<>();
    int currentSize = 0;
    while (currentSize < requestSize) {
      DataBatch elem = batches.remove(0);
      retBatches.add(elem);
      currentSize += elem.body.length;
      totalSize -= elem.body.length;
    }
    return retBatches;
  }
}
