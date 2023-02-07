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
package org.apache.drill.yarn.appMaster;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract base class for schedulers that work with persistent
 * (long-running) tasks. Such tasks are intended to run until
 * explicitly shut down (unlike batch tasks that run until
 * some expected completion.)
 * <p>
 * Provides a target quantity of tasks
 * (see {@link #getTarget()}, along with operations to increase,
 * decrease or set the target number.
 * <p>
 * The scheduler acts as a controller: starting new tasks as needed to
 * match the desired target, or stopping tasks as needed when the
 * target level is reduced.
 */

public abstract class PersistentTaskScheduler extends AbstractScheduler {
  private static final Log LOG = LogFactory.getLog(PersistentTaskScheduler.class);
  protected int quantity;

  public PersistentTaskScheduler(String type, String name, int quantity) {
    super(type, name);
    this.quantity = quantity;
  }

  /**
   * Set the number of running tasks to the quantity given.
   *
   * @param level
   *          the target number of tasks
   */

  @Override
  public int resize(int level) {
    quantity = level;
    if (quantity < 0) {
      quantity = 0;
    }
    return quantity;
  }

  @Override
  public int getTarget() { return quantity; }

  /**
   * Indicate that a task is completed. Normally occurs only
   * when shutting down excess tasks.
   *
   * @param task
   */


  @Override
  public void completed(Task task) { }

  /**
   * Progress for persistent tasks defaults to the ratio of
   * running tasks to target level. Thus, a persistent cluster
   * will normally report 100% progress.
   *
   * @return The progress of persistent tasks.
   */

  @Override
  public int[] getProgress() {
    int activeCount = state.getTaskCount();
    return new int[] { Math.min(activeCount, quantity), quantity };
  }

  /**
   * Adjust the number of running tasks to better match the target
   * by starting or stopping tasks as needed.
   */

  @Override
  public void adjust() {
    int activeCount = state.getTaskCount();
    int delta = quantity - activeCount;
    if (delta > 0) {
      addTasks(delta);
    } else if (delta < 0) {
      cancelTasks(activeCount);
    }
  }

  /**
   * Cancel the requested number of tasks. We exclude any tasks that are already
   * in the process of being cancelled. Because we ignore those tasks, it might
   * be that we want to reduce the task count, but there is nothing left to cancel.
   *
   * @param cancelCount
   */

  private void cancelTasks(int cancelCount) {
    int cancelled = state.getCancelledTaskCount();
    int cancellable = cancelCount - cancelled;
    int n = cancellable - quantity;
    LOG.info("[" + getName( ) + "] - Cancelling " + cancelCount +
             " tasks. " + cancelled + " are already cancelled, " +
             cancellable + " more will be cancelled.");
    if (n <= 0) {
      return;
    }
    for (Task task : state.getStartingTasks()) {
      state.cancel(task);
      if (--n == 0) {
        return;
      }
    }
    for (Task task : state.getActiveTasks()) {
      state.cancel(task);
      if (--n == 0) {
        return;
      }
    }

    // If we get here it means something has gotten out of whack.

    LOG.error("Tried to cancel " + cancellable + " tasks, but " + n + " could not be cancelled.");
    assert false;
  }

  /**
   * The persistent scheduler has no fixed sequence of tasks to run, it launches
   * a set and is never "done". For purposes of completion tracking claim we
   * have no further tasks.
   *
   * @return false
   */

  @Override
  public boolean hasMoreTasks() { return false; }

  @Override
  public void requestTimedOut() {

    // We requested a node a while back, requested a container from YARN,
    // but waited too long to receive it. Most likely cause is that we
    // want a container on a node that either does not exist, or is too
    // heavily loaded. (That is, we have a 3-node cluster and are requesting
    // a 4th node. Or, we have 2 nodes but node 3 has insufficient resources.)
    // In either case, we're not likely to ever get the container, so just
    // reduce the target size to what we an get.

    assert quantity > 0;
    if (quantity == 0) {
      LOG.error("Container timed out, but target quantity is already 0!");
    } else {
      quantity--;
      LOG.info("Container request timed out. Reducing target container count by 1 to " + quantity);
    }
  }
}
