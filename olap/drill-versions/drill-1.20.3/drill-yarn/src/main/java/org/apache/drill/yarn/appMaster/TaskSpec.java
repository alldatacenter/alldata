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

import org.apache.drill.yarn.core.ContainerRequestSpec;
import org.apache.drill.yarn.core.LaunchSpec;

public class TaskSpec {
  /**
   * Number of YARN vcores (virtual cores) and amount of memory (in MB) needed
   * by this task.
   */

  public ContainerRequestSpec containerSpec;

  /**
   * Description of of the task process, environment and so on.
   */

  public LaunchSpec launchSpec;

  public int maxRetries;

  public String name;
}
