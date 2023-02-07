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
package org.apache.drill.exec.rpc.control;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.apache.drill.exec.proto.BitControl.FragmentStatus;
import org.apache.drill.exec.proto.ExecProtos.FragmentHandle;
import org.apache.drill.exec.proto.UserBitShared.QueryId;
import org.apache.drill.exec.proto.helper.QueryIdHelper;
import org.apache.drill.exec.work.foreman.FragmentStatusListener;
import org.apache.drill.exec.work.foreman.ForemanSetupException;
import org.apache.drill.exec.work.fragment.FragmentManager;

import org.apache.drill.shaded.guava.com.google.common.collect.Maps;

public class WorkEventBus {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(WorkEventBus.class);
  private final ConcurrentMap<FragmentHandle, FragmentManager> managers = Maps.newConcurrentMap();
  private final ConcurrentMap<QueryId, FragmentStatusListener> listeners =
      new ConcurrentHashMap<>(16, 0.75f, 16);

  public void removeFragmentStatusListener(final QueryId queryId) {
    if (logger.isDebugEnabled()) {
      logger.debug("Removing fragment status listener for queryId {}.", QueryIdHelper.getQueryId(queryId));
    }
    listeners.remove(queryId);
  }

  public void addFragmentStatusListener(final QueryId queryId, final FragmentStatusListener listener)
      throws ForemanSetupException {
    if (logger.isDebugEnabled()) {
      logger.debug("Adding fragment status listener for queryId {}.", QueryIdHelper.getQueryId(queryId));
    }
    final FragmentStatusListener old = listeners.putIfAbsent(queryId, listener);
    if (old != null) {
      throw new ForemanSetupException (
          "Failure.  The provided handle already exists in the listener pool.  You need to remove one listener before adding another.");
    }
  }

  public void statusUpdate(final FragmentStatus status) {
    final FragmentStatusListener listener = listeners.get(status.getHandle().getQueryId());
    if (listener == null) {
      logger.warn("A fragment message arrived but there was no registered listener for that message: {}.", status);
    } else {
      listener.statusUpdate(status);
    }
  }

  public void addFragmentManager(final FragmentManager fragmentManager) {
    if (logger.isDebugEnabled()) {
      logger.debug("Fragment {} manager created: {}", QueryIdHelper.getQueryIdentifier(fragmentManager.getHandle()), fragmentManager);
    }
    final FragmentManager old = managers.putIfAbsent(fragmentManager.getHandle(), fragmentManager);
    if (old != null) {
      throw new IllegalStateException(
          String.format("Manager %s for fragment %s already exists.", old, QueryIdHelper.getQueryIdentifier(fragmentManager.getHandle())));
    }
  }

  public FragmentManager getFragmentManager(final FragmentHandle handle) {
    return managers.get(handle);
  }

  /**
   * Optionally cancels and removes fragment manager (for the corresponding the handle) from the work event bus. Currently, used
   * for fragments waiting on data (root and intermediate). This method can be called multiple times. The manager will be removed
   * only once (the first call).
   * @param handle the handle to the fragment
   * @param cancel
   * @return if the fragment was found and removed from the event bus
   */
  public boolean removeFragmentManager(final FragmentHandle handle, final boolean cancel) {
    final FragmentManager manager = managers.remove(handle);
    if (manager != null) {
      assert !manager.isCancelled() : String.format("Fragment %s manager %s is already cancelled.", QueryIdHelper.getQueryIdentifier(handle), manager);
      if (cancel) {
        manager.cancel();
      }
      if (logger.isDebugEnabled()) {
        logger.debug("{} fragment {} manager {} from the work bus.", cancel ? "Cancel and removed" : "Removed",
            QueryIdHelper.getQueryIdentifier(handle), manager);
      }
      return true;
    } else if (logger.isWarnEnabled()) {
      logger.warn("Fragment {} manager is not found in the work bus.", QueryIdHelper.getQueryIdentifier(handle));
    }
    return false;
  }
}
