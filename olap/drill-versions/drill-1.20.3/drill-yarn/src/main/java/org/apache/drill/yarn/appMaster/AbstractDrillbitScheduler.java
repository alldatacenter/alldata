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
import org.apache.drill.yarn.zk.ZKRegistry;

/**
 * Base class for schedulers (pools) for Drillbits. Derived classes implement
 * various policies for node selection. This class handles the common tasks such
 * as holding the Drillbit launch specification, providing Drillbit- specific
 * behaviors and so on.
 * <p>
 * The key purpose of this class is to abstract Drillbit-speicific code from the
 * rest of the AM cluster controller. We do so for several reasons: ease of
 * testing (we can use mock tasks), ability to handle additional server types in
 * the future, and a way to keep each module focused on a single task (as the
 * controller and its state machine is complex enough without mixing in Drillbit
 * specifics.)
 */

public abstract class AbstractDrillbitScheduler
    extends PersistentTaskScheduler {
  /**
   * Interface to provide Drill-bit specific behavior. Ideally, this class would
   * provide the interface to gracefully shut down a Drillbit, but Drill has no
   * API to do graceful shutdown in this release. (The only graceful shutdown is
   * by issuing a SIGTERM from the node runing the Drillbit, but YARN has no way
   * to do this, despite active discussions on several YARN JIRA entries.
   */

  public class DrillbitManager extends AbstractTaskManager {
    /**
     * Allow only one concurrent container request by default to ensure that the
     * node blacklist mechanism works to ensure that the RM does not allocate
     * two containers on the same node.
     */

    @Override
    public int maxConcurrentAllocs() {
      return 1;
    }

    @Override
    public void allocated(EventContext context) {

      // One drillbit per node, so reserve the node
      // just allocated.

      context.controller.getNodeInventory().reserve(context.task.container);
    }

    @Override
    public void completed(EventContext context) {
      // This method is called for all completed tasks, even those that
      // completed (were cancelled) before a container was allocated.
      // If we have no container, then we have nothing to tell the
      // node inventory.

      if (context.task.container != null) {
        context.controller.getNodeInventory().release(context.task.container);
      }
      analyzeResult(context);
    }

    @Override
    public boolean isLive(EventContext context) {
      ZKRegistry reg = (ZKRegistry) context.controller.getProperty(ZKRegistry.CONTROLLER_PROPERTY);
      return reg.isRegistered(context.task);
    }

    /**
     * Analyze the result. Drillbits should not exit, but this one did. It might
     * be because we asked it to exit, which is fine. Otherwise, the exit is
     * unexpected and we should 1) provide the admin with an explanation, and 2)
     * prevent retries after a few tries.
     *
     * @param context
     */

    private void analyzeResult(EventContext context) {
      Task task = context.task;

      // If we cancelled the Drill-bit, just unblacklist the
      // host so we can run another drillbit on it later.

      if (task.isCancelled()) {
        return;
      }

      // The Drill-bit stopped on its own.
      // Maybe the exit status will tell us something.

      int exitCode = task.completionStatus.getExitStatus();

      // We can also consider the runtime.

      long duration = task.uptime() / 1000;

      // The ZK state may also help.

      boolean registered = task.trackingState != Task.TrackingState.NEW;

      // If the exit code was 1, then the script probably found
      // an error. Only retry once.

      if (registered || task.getTryCount() < 2) {

        // Use the default retry policy.

        return;
      }

      // Seems to be a mis-configuration. The Drill-bit exited quickly and
      // did not register in ZK. Also, we've tried twice now with no luck.
      // Assume the node is bad.

      String hostName = task.getHostName();
      StringBuilder buf = new StringBuilder();
      buf.append(task.getLabel()).append(" on host ").append(hostName)
          .append(" failed with status ").append(exitCode).append(" after ")
          .append(duration).append(" secs. with");
      if (!registered) {
        buf.append("out");
      }
      buf.append(" ZK registration");
      if (duration < 60 && !registered) {
        buf.append(
            "\n    Probable configuration problem, check Drill log file on host ")
            .append(hostName).append(".");
      }
      LOG.error(buf.toString());
      task.cancelled = true;

      // Mark the host as permanently blacklisted. Leave it
      // in YARN's blacklist.

      context.controller.getNodeInventory().blacklist(hostName);
    }
  }

  private static final Log LOG = LogFactory
      .getLog(AbstractDrillbitScheduler.class);

  public AbstractDrillbitScheduler(String type, String name, int quantity) {
    super(type, name, quantity);
    isTracked = true;
    setTaskManager(new DrillbitManager());
  }
}
