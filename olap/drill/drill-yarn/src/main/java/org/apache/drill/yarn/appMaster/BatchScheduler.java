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

public class BatchScheduler extends AbstractScheduler {
  private int quantity;
  private int completedCount;

  public BatchScheduler(String name, int quantity) {
    super("batch", name);
    this.quantity = quantity;
  }

  @Override
  public void completed(Task task) {
    completedCount++;
    if (task.getDisposition() != Task.Disposition.COMPLETED) {
      failCount++;
    }
  }

  @Override
  public int resize(int level) {
    quantity = level;
    return quantity;
  }

  @Override
  public int getTarget() { return quantity; }

  @Override
  public int[] getProgress() {
    return new int[] { Math.min(completedCount, quantity), quantity };
  }

  @Override
  public void adjust() {
    int activeCount = state.getTaskCount();
    int delta = quantity - activeCount - completedCount;
    if (delta < 0) {
      addTasks(-delta);
    }
    if (delta > 0) {
      cancelTasks(delta);
    }
  }

  /**
   * Cancel any starting tasks. We don't cancel launched, in-flight tasks
   * because there is no way to tell YARN to cancel tasks that are in the
   * process of being launched: we have to wait for them to start
   * before canceling.
   *
   * @param n
   */

  private void cancelTasks(int n) {
    for (Task task : state.getStartingTasks()) {
      state.cancel(task);
      if (--n == 0) {
        break;
      }
    }
  }

  @Override
  public boolean hasMoreTasks() {
    return completedCount < quantity;
  }

  @Override
  public void requestTimedOut() {
    // Not clear what to do here. Since this case is used only for testing,
    // deal with this case later.
  }
}
