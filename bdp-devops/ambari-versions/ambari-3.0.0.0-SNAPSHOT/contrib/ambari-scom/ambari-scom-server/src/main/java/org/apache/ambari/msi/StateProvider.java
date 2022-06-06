/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.msi;

/**
 * Interface to provide component state to the MSI resource provider.
 */
public interface StateProvider {
  /**
   * Determine whether or not the host component identified by the given host name
   * and component name is running.
   *
   * @param hostName       the host name
   * @param componentName  the component name
   *
   * @return true if the host component is healthy
   */
  public State getRunningState(String hostName, String componentName);

  /**
   * Set the running state of the given component.
   *
   * @param hostName       the host name
   * @param componentName  the component name
   * @param state          the desired state
   */
  public Process setRunningState(String hostName, String componentName, State state);

  /**
   * Enum of possible states.
   */
  public enum State {
    Stopped,
    Running,
    Paused,
    Unknown
  }

  public interface Process {
    public boolean isRunning();

    public int getExitCode();

    public String getOutput();

    public String getError();
  }

}
