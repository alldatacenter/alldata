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
package org.apache.drill.exec.work.foreman;

import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared.FragmentState;
import org.apache.drill.exec.proto.UserBitShared.MinorFragmentProfile;
import org.apache.drill.exec.proto.UserBitShared.OperatorProfile;

public class FragmentData {
//  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(FragmentData.class);

  private final boolean isLocal;
  private volatile FragmentStatus status;
  private volatile long lastStatusUpdate = System.currentTimeMillis();
  private volatile long lastProgress = System.currentTimeMillis();
  private final DrillbitEndpoint endpoint;

  public FragmentData(final FragmentHandle handle, final DrillbitEndpoint endpoint, final boolean isLocal) {
    this.endpoint = endpoint;
    this.isLocal = isLocal;
    final MinorFragmentProfile f = MinorFragmentProfile.newBuilder()
        .setState(FragmentState.SENDING)
        .setMinorFragmentId(handle.getMinorFragmentId())
        .setEndpoint(endpoint)
        .build();
    status = FragmentStatus.newBuilder()
        .setHandle(handle)
        .setProfile(f)
        .build();
  }

  /**
   * Update the status for this fragment. Also records last update and last progress time.
   * @param newStatus Updated status
   */
  public void setStatus(final FragmentStatus newStatus) {
    final long time = System.currentTimeMillis();
    lastStatusUpdate = time;
    if (madeProgress(status, newStatus)) {
      lastProgress = time;
    }
    status = newStatus;
  }

  public FragmentState getState() {
    return status.getProfile().getState();
  }

  public MinorFragmentProfile getProfile() {
    return status
        .getProfile()
        .toBuilder()
        .setLastUpdate(lastStatusUpdate)
        .setLastProgress(lastProgress)
        .build();
  }

  public boolean isLocal() {
    return isLocal;
  }

  public DrillbitEndpoint getEndpoint() {
    return endpoint;
  }

  public FragmentHandle getHandle() {
    return status.getHandle();
  }

  private boolean madeProgress(final FragmentStatus prev, final FragmentStatus cur) {
    final MinorFragmentProfile previous = prev.getProfile();
    final MinorFragmentProfile current = cur.getProfile();

    if (previous.getState() != current.getState()) {
      return true;
    }

    if (previous.getOperatorProfileCount() != current.getOperatorProfileCount()) {
      return true;
    }

    for(int i =0; i < current.getOperatorProfileCount(); i++){
      if (madeProgress(previous.getOperatorProfile(i), current.getOperatorProfile(i))) {
        return true;
      }
    }

    return false;
  }

  private boolean madeProgress(final OperatorProfile prev, final OperatorProfile cur) {
    return prev.getInputProfileCount() != cur.getInputProfileCount()
        || !prev.getInputProfileList().equals(cur.getInputProfileList())
        || prev.getMetricCount() != cur.getMetricCount()
        || !prev.getMetricList().equals(cur.getMetricList());
  }

  @Override
  public String toString() {
    return "FragmentData [isLocal=" + isLocal + ", status=" + status + ", lastStatusUpdate=" + lastStatusUpdate
        + ", endpoint=" + endpoint + "]";
  }
}