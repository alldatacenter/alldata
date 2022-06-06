/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.agent;

import org.apache.ambari.server.AmbariException;

public abstract class AgentReport<R> {

  private final String hostName;
  private final R report;

  public AgentReport(String hostName, R report) {
    this.hostName = hostName;
    this.report = report;
  }

  public String getHostName() {
    return hostName;
  }

  public final void process() throws AmbariException {
    process(report, hostName);
  }

  protected abstract void process(R report, String hostName) throws AmbariException;
}
