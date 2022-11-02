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

package org.apache.ambari.view.cluster;

import java.util.List;
import java.util.Map;

/**
 * View associated cluster.  A cluster may be associated with a view instance so that the view instance may pull
 * configuration values from the cluster.
 */
public interface Cluster {
  /**
   * Get the cluster name.
   *
   * @return the cluster name
   */
  public String getName();

  /**
   * Get a value for the given configuration type and key.
   *
   * @param type  the configuration id  (i.e. hdfs-site)
   * @param key   the configuration key (i.e. dfs.namenode.http-address)
   *
   * @return the configuration value
   */
  public String getConfigurationValue(String type, String key);

  /**
   * @param type : the type (site) for which the configurations are required.
   * @return : return a map containing all the key values of configurations
   */
  public Map<String,String> getConfigByType(String type);
  /**
   * Get the hosts for service and componet
   *
   * @param serviceName
   * @param componentName
   * @return list of hosts
   */
  public List<String> getHostsForServiceComponent(String serviceName, String componentName);
}
