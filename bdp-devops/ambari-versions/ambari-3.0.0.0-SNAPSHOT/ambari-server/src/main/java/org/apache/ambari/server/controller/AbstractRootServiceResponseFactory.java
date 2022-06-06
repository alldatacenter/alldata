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


package org.apache.ambari.server.controller;

import java.util.Set;

import org.apache.ambari.server.AmbariException;

public abstract class AbstractRootServiceResponseFactory {

  /**
   * Get all root services.
   * 
   * @param request the root services 
   * 
   * @return a set of root services 
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public abstract Set<RootServiceResponse> getRootServices(RootServiceRequest request) throws AmbariException;
  
  /**
   * Get all components of root services.
   * 
   * @param request the host components of root services
   * 
   * @return a set of components of root services
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public abstract Set<RootServiceComponentResponse> getRootServiceComponents(RootServiceComponentRequest request) throws AmbariException;
  
  /**
   * Get all components of root services related to hosts.
   * 
   * @param request the host components of root services related to hosts
   * @param hosts the list of hosts
   * 
   * 
   * @return a set of components of root services related to hosts
   * 
   * @throws  AmbariException if the resources cannot be read
   */
  public abstract Set<RootServiceHostComponentResponse> getRootServiceHostComponent(RootServiceHostComponentRequest request,
      Set<HostResponse> hosts) throws AmbariException;
}
