/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.coordinator;

import java.io.Closeable;
import java.util.List;
import java.util.Set;

import org.apache.uniffle.common.config.Reconfigurable;

public interface ClusterManager extends Closeable, Reconfigurable {

  /**
   * Add a server to the cluster.
   *
   * @param shuffleServerInfo server info
   */
  void add(ServerNode shuffleServerInfo);

  /**
   * Get available nodes from the cluster
   *
   * @param requiredTags tags for filter
   * @return list of available server nodes
   */
  List<ServerNode> getServerList(Set<String> requiredTags);

  /**
   * @return number of server nodes in the cluster
   */
  int getNodesNum();

  /**
   * @return list all server nodes in the cluster
   */
  List<ServerNode> list();

  int getShuffleNodesMax();

  /**
   * @return whether to be ready for serving
   */
  boolean isReadyForServe();
}
