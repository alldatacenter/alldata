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
 * Test state provider.
 */
public class TestStateProvider implements StateProvider {

  private State state = State.Running;

  public void setHealthy(boolean healthy) {
    state = healthy ? State.Running : State.Stopped;
  }

  public void setState(State state) {
    this.state = state;
  }

  public State getState() {
    return state;
  }

  @Override
  public State getRunningState(String hostName, String componentName) {
    return state;
  }

  @Override
  public Process setRunningState(String hostName, String componentName, State state) {
    this.state = state;
    return new TestProcess();
  }

  private class TestProcess implements Process {

    @Override
    public boolean isRunning() {
      return false;
    }

    @Override
    public int getExitCode() {
      return 0;
    }

    @Override
    public String getOutput() {
      return "output";
    }

    @Override
    public String getError() {
      return "error";
    }
  }
}
