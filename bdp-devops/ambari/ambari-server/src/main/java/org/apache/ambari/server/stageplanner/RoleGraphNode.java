/*
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
package org.apache.ambari.server.stageplanner;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;

public class RoleGraphNode {
  public RoleGraphNode(Role role, RoleCommand command) {
    this.role = role;
    this.command = command;
  }
  private Role role;
  private RoleCommand command;
  private int inDegree = 0;
  private List<String> hosts = new ArrayList<>();
  private Map<String, RoleGraphNode> edges = new TreeMap<>();
  public synchronized void addHost(String host) {
    hosts.add(host);
  }
  public synchronized void addEdge(RoleGraphNode rgn) {
    if (edges.containsKey(rgn.getRole().toString())) {
      return;
    }
    edges.put(rgn.getRole().toString(), rgn);
    rgn.incrementInDegree();
  }
  private synchronized void incrementInDegree() {
    inDegree ++;
  }
  public Role getRole() {
    return role;
  }
  public RoleCommand getCommand() {
    return command;
  }
  public List<String> getHosts() {
    return hosts;
  }
  public int getInDegree() {
    return inDegree;
  }
  
  Collection<RoleGraphNode> getEdges() {
    return edges.values();
  }
  public synchronized void decrementInDegree() {
    inDegree --;
  }
  
  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    builder.append("(").append(role).append(", ").append(command).append(", ").append(inDegree).append(")");
    return builder.toString();
  }
}
