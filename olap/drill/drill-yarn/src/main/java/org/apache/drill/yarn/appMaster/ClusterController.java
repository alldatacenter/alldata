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

import java.util.List;

import org.apache.hadoop.yarn.api.records.Container;
import org.apache.hadoop.yarn.api.records.ContainerId;
import org.apache.hadoop.yarn.api.records.ContainerStatus;

/**
 * Interface which identifies the cluster controller methods that are save to
 * call from the {@link Dispatcher}. Methods here are either designed to be
 * called before the event threads start or after they complete. The remainder
 * synchronized to coordinate between event threads.
 */

public interface ClusterController extends RegistryHandler {
  void enableFailureCheck(boolean flag);

  void registerLifecycleListener(TaskLifecycleListener listener);

  void registerScheduler(Scheduler resourceGroup);

  void setProperty(String key, Object value);

  Object getProperty(String key);

  /**
   * Called after the dispatcher has started YARN and other server
   * components. The controller can now begin to spin up tasks.
   */

  void started( ) throws YarnFacadeException, AMException;

  /**
   * Called by the timer ("pulse") thread to trigger time-based events.
   *
   * @param curTime
   */

  void tick(long curTime);

  /**
   * The RM has allocated one or more containers in response to container
   * requests submitted to the RM.
   *
   * @param containers
   *          the set of containers provided by YARN
   */

  void containersAllocated(List<Container> containers);

  /**
   * The NM reports that a container has successfully started.
   *
   * @param containerId
   *          the container which started
   */

  void containerStarted(ContainerId containerId);

  /**
   * The RM API reports that an attempt to start a container has failed locally.
   *
   * @param containerId
   *          the container that failed to launch
   * @param t
   *          the error that occurred
   */

  void taskStartFailed(ContainerId containerId, Throwable t);

  /**
   * The Node Manager reports that a container has stopped.
   *
   * @param containerId
   */
  void containerStopped(ContainerId containerId);

  /**
   * The Resource Manager reports that containers have completed with the given
   * statuses. Find the task for each container and mark them as completed.
   *
   * @param statuses
   */

  void containersCompleted(List<ContainerStatus> statuses);

  float getProgress();

  /**
   * The Node Manager API reports that a request sent to the NM to stop a task
   * has failed.
   *
   * @param containerId
   *          the container that failed to stop
   * @param t
   *          the reason that the stop request failed
   */

  void stopTaskFailed(ContainerId containerId, Throwable t);

  /**
   * Request to resize the Drill cluster by a relative amount.
   *
   * @param delta
   *          the amount of change. Can be positive (to grow) or negative (to
   *          shrink the cluster)
   */

  void resizeDelta(int delta);

  /**
   * Request to resize the Drill cluster to the given size.
   *
   * @param n
   *          the desired cluster size
   */

  int resizeTo(int n);

  /**
   * Indicates a request to gracefully shut down the cluster.
   */

  void shutDown();

  /**
   * Called by the main thread to wait for the normal shutdown of the
   * controller. Such shutdown occurs when the admin sends a sutdown
   * command from the UI or REST API.
   */

  boolean waitForCompletion();

  void updateRMStatus();

  void setMaxRetries(int value);

  /**
   * Allow an observer to see a consistent view of the controller's
   * state by performing the visit in a synchronized block.
   * @param visitor
   */

  void visit( ControllerVisitor visitor );

  /**
   * Allow an observer to see a consistent view of the controller's
   * task state by performing the visit in a synchronized block.
   *
   * @param visitor
   */

  void visitTasks( TaskVisitor visitor );

  /**
   * Return the target number of tasks that the controller seeks to maintain.
   * This is the sum across all pools.
   */

  int getTargetCount();

  boolean isTaskLive(int id);

  /**
   * Cancels the given task, reducing the target task count. Called
   * from the UI to allow the user to select the specific task to end
   * when reducing cluster size.
   *
   * @param id
   */

  boolean cancelTask(int id);

  /**
   * Whether this distribution of YARN supports disk resources.
   * @return True if this distribution of YARN supports disk resources. False otherwise.
   */

  boolean supportsDiskResource();

  int getFreeNodeCount();
}
