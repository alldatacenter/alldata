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
package org.apache.drill.yarn.zk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.drill.shaded.guava.com.google.common.annotations.VisibleForTesting;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.drill.exec.proto.CoordinationProtos.DrillbitEndpoint;
import org.apache.drill.exec.work.foreman.DrillbitStatusListener;
import org.apache.drill.yarn.appMaster.AMWrapperException;
import org.apache.drill.yarn.appMaster.EventContext;
import org.apache.drill.yarn.appMaster.Pollable;
import org.apache.drill.yarn.appMaster.RegistryHandler;
import org.apache.drill.yarn.appMaster.Task;
import org.apache.drill.yarn.appMaster.TaskLifecycleListener;

/**
 * AM-specific implementation of a Drillbit registry backed by ZooKeeper.
 * Listens to ZK events for registering a Drillbit and deregistering. Alerts the
 * Cluster Controller of these events.
 * <p>
 * Locking strategy: Receives events from both ZK and the cluster controller,
 * both of which must be synchronized. To prevent deadlocks, this class NEVER
 * calls into the cluster controller while holding a lock. This prevents the
 * following:
 * <p>
 * ClusterController --> ZKRegistry (OK) <br>
 * ZK --> ZKRegistry (OK) <br>
 * ZK --> ZKRegistry --> Cluster Controller (bad)
 * <p>
 * In the case of registration, ZK calls the registry which must alert the
 * cluster controller. Cluster controller alerting is handled outside the ZK
 * update critical section.
 * <p>
 * Because ZK events are occur relatively infrequently, any deadlock will occur
 * once in a blue moon, which will make it very hard to reproduce. So, extreme
 * caution is needed at design time to prevent the problem.
 */

public class ZKRegistry
    implements TaskLifecycleListener, DrillbitStatusListener, Pollable {
  /**
   * State of each Drillbit that we've discovered through ZK or launched via the
   * AM. The tracker is where we combine the ZK information with AM to correlate
   * overall Drillbit health.
   */

  protected static class DrillbitTracker {
    /**
     * A Drillbit can be in one of four states.
     */

    public enum State {

      /**
       * An unmanaged Drillbit is one that has announced itself via ZK, but
       * which the AM didn't launch (or has not yet received confirmation from
       * YARN that it was launched.) In the normal state, this state either does
       * not occur (YARN reports the task launch before the Drillbit registers
       * in ZK) or is transient (if the Drillbit registers in ZK before YARN
       * gets around to telling the AM that the Drillbit was launched.) A
       * Drillbit that stays in the unregistered state is likely one launched
       * outside the AM: either launched manually or (possibly), one left from a
       * previous, failed AM run (though YARN is supposed to kill left-over
       * child processes in that case.)
       */

      UNMANAGED,

      /**
       * A new Drillbit is one that the AM has launched, but that has not yet
       * registered itself with ZK. This is normally a transient state that
       * occurs as ZK registration catches up with the YARN launch notification.
       * If a Drillbit says in this state, then something is seriously wrong
       * (perhaps a mis-configuration). The cluster controller will patiently
       * wait a while, then decide bad things are happening and will ask YARN to
       * kill the Drillbit, then will retry a time or two, after which it will
       * throw up its hands, blacklist the node, and wait for the admin to sort
       * things out.
       */

      NEW,

      /**
       * Normal operating state: the AM launched the Drillbit, which then
       * dutifully registered itself in ZK. Nothing to see here, move along.
       */

      REGISTERED,

      /**
       * The Drillbit was working just fine, but its registration has dropped
       * out of ZK for a reason best left to the cluster controller to
       * determine. Perhaps the controller has decided to kill the Drillbit.
       * Perhaps the Drillbit became unresponsive (in which case the controller
       * will kill it and retry) or has died (in which case YARN will alert the
       * AM that the process exited.)
       */

      DEREGISTERED
    }

    /**
     * The common key used between tasks and ZK registrations. The key is of the
     * form:<br>
     *
     * <pre>
     * host:port:port:port
     * </pre>
     */

    protected final String key;

    /**
     * ZK tracking state.
     *
     * See {@link org.apache.drill.yarn.zk.ZKRegistry.DrillbitTracker.State}
     */

    protected State state;

    /**
     * For Drillbits started by the AM, the task object for this Drillbit.
     */

    protected Task task;

    /**
     * For Drillbits discovered through ZK, the Drill endpoint for the Drillbit.
     */

    protected DrillbitEndpoint endpoint;

    public DrillbitTracker(String key, DrillbitEndpoint endpoint) {
      this.key = key;
      this.state = DrillbitTracker.State.UNMANAGED;
      this.endpoint = endpoint;
    }

    public DrillbitTracker(String key, Task task) {
      this.key = key;
      this.task = task;
      state = DrillbitTracker.State.NEW;
    }

    /**
     * Mark that a YARN-managed task has become registered in ZK. This indicates
     * that the task has come online. Tell the task to update its state to
     * record that the task is, in fact, registered in ZK. This indicates a
     * normal, healthy task.
     */

    private void becomeRegistered() {
      state = DrillbitTracker.State.REGISTERED;
    }

    /**
     * Mark that a YARN-managed Drillbit has dropped out of ZK.
     */

    public void becomeUnregistered() {
      assert state == DrillbitTracker.State.REGISTERED;
      state = DrillbitTracker.State.DEREGISTERED;
      endpoint = null;
    }
  }

  public static final String CONTROLLER_PROPERTY = "zk";

  public static final int UPDATE_PERIOD_MS = 20_000;

  public static final String ENDPOINT_PROPERTY = "endpoint";

  private static final Log LOG = LogFactory.getLog(ZKRegistry.class);

  /**
   * Map of host:port:port:port keys to tracking objects. Identifies the
   * Drillbits discovered from ZK, started by the controller, or (ideally) both.
   */

  private Map<String, DrillbitTracker> registry = new HashMap<>();

  /**
   * Interface to Drill's cluster coordinator.
   */

  private ZKClusterCoordinatorDriver zkDriver;

  /**
   * Drill's cluster coordinator (or, at least, Drill-on-YARN's version of it.
   */

  private RegistryHandler registryHandler;

  /**
   * Last check of ZK status.
   */

  private long lastUpdateTime;

  public ZKRegistry(ZKClusterCoordinatorDriver zkDriver) {
    this.zkDriver = zkDriver;
  }

  /**
   * Called during AM startup to initialize ZK. Checks if any Drillbits are
   * already running. These are "unmanaged" because the AM could not have
   * started them (since they predate the AM.)
   */

  public void start(RegistryHandler controller) {
    this.registryHandler = controller;
    try {
      zkDriver.build();
    } catch (ZKRuntimeException e) {
      LOG.error("Failed to start ZK monitoring", e);
      throw new AMWrapperException("Failed to start ZK monitoring", e);
    }
    for (DrillbitEndpoint dbe : zkDriver.getInitialEndpoints()) {
      String key = toKey(dbe);
      registry.put(key, new DrillbitTracker(key, dbe));

      // Blacklist the host for each unmanaged drillbit.

      controller.reserveHost(dbe.getAddress());

      LOG.warn("Host " + dbe.getAddress()
          + " already running a Drillbit outside of YARN.");
    }
    zkDriver.addDrillbitListener(this);
  }

  /**
   * Convert a Drillbit endpoint to a string key used in the (zk-->task) map.
   * Note that the string format here must match the one used in
   * {@link #toKey(Task)} to map a task to string key.
   *
   * @param dbe
   *          the Drillbit endpoint from ZK
   * @return a string key for this object
   */

  private String toKey(DrillbitEndpoint dbe) {
    return ZKClusterCoordinatorDriver.asString(dbe);
  }

  /**
   * Convert a task to a string key used in the (zk-->task) map. Note that the
   * string format here must match the one used in
   * {@link #toKey(DrillbitEndpoint)} to map a drillbit endpoint to string key.
   *
   * @param task
   *          the task tracked by the cluster controller
   * @return a string key for this object
   */

  private String toKey(Task task) {
    return zkDriver.toKey(task.getHostName());
  }

  // private String toKey(Container container) {
  // return zkDriver.toKey(container.getNodeId().getHost());
  // }

  public static class AckEvent {
    Task task;
    DrillbitEndpoint endpoint;

    public AckEvent(Task task, DrillbitEndpoint endpoint) {
      this.task = task;
      this.endpoint = endpoint;
    }
  }

  /**
   * Callback from ZK to indicate that one or more drillbits have become
   * registered. We handle registrations in a critical section, then alert the
   * cluster controller outside the critical section.
   */

  @Override
  public void drillbitRegistered(Set<DrillbitEndpoint> registeredDrillbits) {
    List<AckEvent> updates = registerDrillbits(registeredDrillbits);
    for (AckEvent event : updates) {
      if (event.task == null) {
        registryHandler.reserveHost(event.endpoint.getAddress());
      } else {
        registryHandler.startAck(event.task, ENDPOINT_PROPERTY, event.endpoint);
      }
    }
  }

  private synchronized List<AckEvent> registerDrillbits(
      Set<DrillbitEndpoint> registeredDrillbits) {
    List<AckEvent> events = new ArrayList<>();
    for (DrillbitEndpoint dbe : registeredDrillbits) {
      AckEvent event = drillbitRegistered(dbe);
      if (event != null) {
        events.add(event);
      }
    }
    return events;
  }

  /**
   * Called when a drillbit has become registered. There are two cases. Either
   * this is a normal registration of a previously-started task, or this is an
   * unmanaged drillbit for which we have no matching task.
   */

  private AckEvent drillbitRegistered(DrillbitEndpoint dbe) {
    String key = toKey(dbe);
    DrillbitTracker tracker = registry.get(key);
    if (tracker == null) {
      // Unmanaged drillbit case

      LOG.info("Registration of unmanaged drillbit: " + key);
      tracker = new DrillbitTracker(key, dbe);
      registry.put(key, tracker);
      return new AckEvent(null, dbe);
    }

    // Managed drillbit case. Might be we lost, then regained
    // ZK connection.

    if (tracker.state == DrillbitTracker.State.REGISTERED) {
      LOG.info("Re-registration of known drillbit: " + key);
      return null;
    }

    // Otherwise, the Drillbit has just registered with ZK.
    // Or, if the ZK connection was lost and regained, the
    // state changes from DEREGISTERED --> REGISTERED

    LOG.info("Drillbit registered: " + key + ", task: " + tracker.task.toString() );
    tracker.endpoint = dbe;
    tracker.becomeRegistered();
    return new AckEvent(tracker.task, dbe);
  }

  /**
   * Callback from ZK to indicate that one or more drillbits have become
   * deregistered from ZK. We handle the deregistrations in a critical section,
   * but updates to the cluster controller outside of a critical section.
   */

  @Override
  public void drillbitUnregistered(
      Set<DrillbitEndpoint> unregisteredDrillbits) {
    List<AckEvent> updates = unregisterDrillbits(unregisteredDrillbits);
    for (AckEvent event : updates) {
      registryHandler.completionAck(event.task, ENDPOINT_PROPERTY);
    }
  }

  private synchronized List<AckEvent> unregisterDrillbits(
      Set<DrillbitEndpoint> unregisteredDrillbits) {
    List<AckEvent> events = new ArrayList<>();
    for (DrillbitEndpoint dbe : unregisteredDrillbits) {
      AckEvent event = drillbitUnregistered(dbe);
      if (event != null) {
        events.add(event);
      }
    }
    return events;
  }

  /**
   * Handle the case that a drillbit becomes unregistered. There are three
   * cases.
   * <ol>
   * <li>The deregistration is for a drillbit that is not in the registry table.
   * Indicates a code error.</li>
   * <li>The drillbit is unmanaged. This occurs for drillbits started and
   * stopped outside of YARN.</li>
   * <li>Normal case of deregistration of a YARN-managed drillbit. Inform the
   * controller of this event.</li>
   * </ol>
   *
   * @param dbe
   */

  private AckEvent drillbitUnregistered(DrillbitEndpoint dbe) {
    String key = toKey(dbe);
    DrillbitTracker tracker = registry.get(key);
    assert tracker != null;
    if (tracker == null) {
      // Something is terribly wrong.
      // Have seen this when a user kills the Drillbit just after it starts. Evidently, the
      // Drillbit registers with ZK just before it is killed, but before DoY hears about
      // the registration.

      LOG.error("Internal error - Unexpected drillbit unregistration: " + key);
      return null;
    }
    if (tracker.state == DrillbitTracker.State.UNMANAGED) {
      // Unmanaged drillbit

      assert tracker.task == null;
      LOG.info("Unmanaged drillbit unregistered: " + key);
      registry.remove(key);
      registryHandler.releaseHost(dbe.getAddress());
      return null;
    }
    LOG.info("Drillbit unregistered: " + key + ", task: " + tracker.task.toString() );
    tracker.becomeUnregistered();
    return new AckEvent(tracker.task, dbe);
  }

  /**
   * Listen for selected YARN task state changes. Called from within the cluster
   * controller's critical section.
   */

  @Override
  public synchronized void stateChange(Event event, EventContext context) {
    switch (event) {
    case ALLOCATED:
      taskCreated(context.task);
      break;
    case ENDED:
      taskEnded(context.task);
      break;
    default:
      break;
    }
  }

  /**
   * Indicates that the cluster controller has created a task that we expect to
   * be monitored by ZK. We handle two cases: the normal case in which we later
   * receive a ZK notification. And, the unusual case in which we've already
   * received the ZK notification and we now match that notification with this
   * task. (The second case could occur if latency causes us to receive the ZK
   * notification before we learn from the NM that the task is alive.)
   *
   * @param task
   */

  private void taskCreated(Task task) {
    String key = toKey(task);
    DrillbitTracker tracker = registry.get(key);
    if (tracker == null) {
      // Normal case: no ZK registration yet.

      registry.put(key, new DrillbitTracker(key, task));
    } else if (tracker.state == DrillbitTracker.State.UNMANAGED) {
      // Unusual case: ZK registration came first.

      LOG.info("Unmanaged drillbit became managed: " + key);
      tracker.task = task;
      tracker.becomeRegistered();

      // Note: safe to call this here as we are already in the controller
      // critical section.

      registryHandler.startAck(task, ENDPOINT_PROPERTY, tracker.endpoint);
    } else {
      LOG.error(task.getLabel() + " - Drillbit registry in wrong state "
          + tracker.state + " for new task: " + key);
    }
  }

  /**
   * Report whether the given task is still registered in ZK. Called while
   * waiting for a deregistration event to catch possible cases where the
   * messages is lost. The message should never be lost, but we've seen
   * cases where tasks hang in this state. This is a potential work-around.
   *
   * @param task
   * @return True if the given task is regestered. False otherwise.
   */

  public synchronized boolean isRegistered(Task task) {
    String key = toKey(task);
    DrillbitTracker tracker = registry.get(key);
    if (tracker==null) {
      return false;
    }
    return tracker.state == DrillbitTracker.State.REGISTERED;
  }

  /**
   * Mark that a task (YARN container) has ended. Updates the (zk --> task)
   * registry by removing the task. The cluster controller state machine
   * monitors ZK and does not end the task until the ZK registration for that
   * task drops. As a result, the entry here should be in the deregistered state
   * or something is seriously wrong.
   *
   * @param task
   */

  private void taskEnded(Task task) {

    // If the task has no host name then the task is being cancelled before
    // a YARN container was allocated. Just ignore such a case.

    if (task.getHostName() == null) {
      return;
    }
    String key = toKey(task);
    DrillbitTracker tracker = registry.get(key);
    assert tracker != null;
    assert tracker.state == DrillbitTracker.State.DEREGISTERED;
    registry.remove(key);
  }

  /**
   * Periodically check ZK status. If the ZK connection has timed out, something
   * is very seriously wrong. Shut the whole Drill cluster down since Drill
   * cannot operate without ZooKeeper.
   * <p>
   * This method should not be synchronized. It checks only the ZK state, not
   * internal state. Further, if we do reconnect to ZK, then a ZK thread may
   * attempt to update this registry, which will acquire a synchronization lock.
   */

  @Override
  public void tick(long curTime) {
    if (lastUpdateTime + UPDATE_PERIOD_MS < curTime) {
      return;
    }

    lastUpdateTime = curTime;
    if (zkDriver.hasFailed()) {
      int secs = (int) ((zkDriver.getLostConnectionDurationMs() + 500) / 1000);
      LOG.error(
          "ZooKeeper connection lost, failing after " + secs + " seconds.");
      registryHandler.registryDown();
    }
  }

  public void finish(RegistryHandler handler) {
    zkDriver.removeDrillbitListener(this);
    zkDriver.close();
  }

  public synchronized List<String> listUnmanagedDrillits() {
    List<String> drillbits = new ArrayList<>();
    for (DrillbitTracker item : registry.values()) {
      if (item.state == DrillbitTracker.State.UNMANAGED) {
        drillbits.add(item.key);
      }
    }
    return drillbits;
  }

  /**
   * Get the current registry for testing. Why for testing? Because this is
   * unsynchronized. In production code, the map may change out from under you.
   *
   * @return The current registry.
   */
  @VisibleForTesting
  protected Map<String, DrillbitTracker> getRegistryForTesting() {
    return registry;
  }
}
