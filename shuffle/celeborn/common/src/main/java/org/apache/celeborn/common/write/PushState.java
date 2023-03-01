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

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.celeborn.common.CelebornConf;
import org.apache.celeborn.common.protocol.PartitionLocation;

public class PushState {

  private final int pushBufferMaxSize;
  public AtomicReference<IOException> exception = new AtomicReference<>();
  private final InFlightRequestTracker inFlightRequestTracker;

  public PushState(CelebornConf conf) {
    pushBufferMaxSize = conf.pushBufferMaxSize();
    inFlightRequestTracker = new InFlightRequestTracker(conf, this);
  }

  public void cleanup() {
    inFlightRequestTracker.cleanup();
  }

  // key: ${master addr}-${slave addr} value: list of data batch
  public final ConcurrentHashMap<String, DataBatches> batchesMap = new ConcurrentHashMap<>();

  /**
   * Not thread-safe
   *
   * @param addressPair
   * @param loc
   * @param batchId
   * @param body
   * @return
   */
  public boolean addBatchData(String addressPair, PartitionLocation loc, int batchId, byte[] body) {
    DataBatches batches = batchesMap.computeIfAbsent(addressPair, (s) -> new DataBatches());
    batches.addDataBatch(loc, batchId, body);
    return batches.getTotalSize() > pushBufferMaxSize;
  }

  public DataBatches takeDataBatches(String addressPair) {
    return batchesMap.remove(addressPair);
  }

  public int nextBatchId() {
    return inFlightRequestTracker.nextBatchId();
  }

  public void addBatch(int batchId, String hostAndPushPort) {
    inFlightRequestTracker.addBatch(batchId, hostAndPushPort);
  }

  public void removeBatch(int batchId, String hostAndPushPort) {
    inFlightRequestTracker.removeBatch(batchId, hostAndPushPort);
  }

  public void onSuccess(String hostAndPushPort) {
    inFlightRequestTracker.onSuccess(hostAndPushPort);
  }

  public void onCongestControl(String hostAndPushPort) {
    inFlightRequestTracker.onCongestControl(hostAndPushPort);
  }

  public boolean limitMaxInFlight(String hostAndPushPort) throws IOException {
    return inFlightRequestTracker.limitMaxInFlight(hostAndPushPort);
  }

  public boolean limitZeroInFlight() throws IOException {
    return inFlightRequestTracker.limitZeroInFlight();
  }

  public boolean reachLimit(String hostAndPushPort, int maxInFlight) throws IOException {
    return inFlightRequestTracker.reachLimit(hostAndPushPort, maxInFlight);
  }
}
