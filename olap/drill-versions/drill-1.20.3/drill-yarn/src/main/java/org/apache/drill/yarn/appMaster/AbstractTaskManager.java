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

import org.apache.drill.yarn.appMaster.Scheduler.TaskManager;
import org.apache.drill.yarn.core.LaunchSpec;

/**
 * Task manager that does nothing.
 */

public class AbstractTaskManager implements TaskManager {
  @Override
  public int maxConcurrentAllocs() {
    return Integer.MAX_VALUE;
  }

  @Override
  public void allocated(EventContext context) {
  }

  @Override
  public LaunchSpec getLaunchSpec(Task task) {
    return task.getLaunchSpec();
  }

  @Override
  public boolean stop(Task task) { return false; }

  @Override
  public void completed(EventContext context) { }

  @Override
  public boolean isLive(EventContext context) { return true; }
}
