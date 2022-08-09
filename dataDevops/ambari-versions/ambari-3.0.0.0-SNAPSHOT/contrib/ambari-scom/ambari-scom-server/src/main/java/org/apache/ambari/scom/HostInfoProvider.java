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
package org.apache.ambari.scom;

import org.apache.ambari.server.controller.spi.SystemException;

/**
 * Provider of host information.
 */
public interface HostInfoProvider {

  /**
   * Get the host name for the given cluster name and component name.
   *
   * @param clusterName    the cluster name
   * @param componentName  the component name
   *
   * @return the host name
   *
   * @throws SystemException if unable to get the host name
   */
  public String getHostName(String clusterName, String componentName)
      throws SystemException;

  /**
   * Get the host name.
   *
   * @param id  the host identifier
   *
   * @return the host name
   *
   * @throws SystemException if unable to get the host name
   */
  public String getHostName(String id)
      throws SystemException;

  /**
   * Get the host ip address.
   *
   * @param id  the host identifier
   *
   * @return the host ip address
   *
   * @throws SystemException if unable to get the host address
   */
  public String getHostAddress(String id)
      throws SystemException;
}
