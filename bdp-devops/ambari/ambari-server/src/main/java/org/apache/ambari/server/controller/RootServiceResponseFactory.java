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

import static org.apache.ambari.server.agent.ExecutionCommand.KeyNames.JDK_LOCATION;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.ObjectNotFoundException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.utils.StageUtils;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.commons.lang.ArrayUtils;

import com.google.inject.Inject;

public class RootServiceResponseFactory extends
    AbstractRootServiceResponseFactory {

  private static final String RUNNING_STATE = "RUNNING";
  public static final String NOT_APPLICABLE = "NOT_APPLICABLE";
  @Inject
  private Configuration configs;
  
  @Inject
  private AmbariMetaInfo ambariMetaInfo;

  @Inject
  private AmbariManagementController managementController;
  
  
  @Override
  public Set<RootServiceResponse> getRootServices(RootServiceRequest request) throws ObjectNotFoundException {
    
    Set<RootServiceResponse> response;
    
    String serviceName = null;
    
    if (request != null)
      serviceName = request.getServiceName();

    if (serviceName != null) {
      RootService service;
      try {
        service = RootService.valueOf(serviceName);
      }
      catch (IllegalArgumentException ex) {
        throw new ObjectNotFoundException("Root service name: " + serviceName);
      }
      
      response = Collections.singleton(new RootServiceResponse(service.toString()));
    } else {
      response = new HashSet<>();
      
      for (RootService service: RootService.values())
        response.add(new RootServiceResponse(service.toString()));
    }    
    return response;
  }
  
  @Override
  public Set<RootServiceComponentResponse> getRootServiceComponents(
      RootServiceComponentRequest request) throws ObjectNotFoundException {
    Set<RootServiceComponentResponse> response = new HashSet<>();

    String serviceName = request.getServiceName();
    String componentName = request.getComponentName();
    RootService service;

    try {
      service = RootService.valueOf(serviceName);
    }
    catch (IllegalArgumentException ex) {
      throw new ObjectNotFoundException("Root service name: " + serviceName);
    }
    catch (NullPointerException np) {
      throw new ObjectNotFoundException("Root service name: null");
    }
    
    if (componentName != null) {
      RootComponent component;
      try {
        component = RootComponent.valueOf(componentName);
        if (!ArrayUtils.contains(service.getComponents(), component))
          throw new ObjectNotFoundException("No component name: " + componentName + "in service: " + serviceName);
      }
      catch (IllegalArgumentException ex) {
        throw new ObjectNotFoundException("Component name: " + componentName);
      }
      response = Collections.singleton(new RootServiceComponentResponse(serviceName, component.toString(),
                                       getComponentVersion(componentName, null),
                                       getComponentProperties(componentName)));
    } else {
    
      for (RootComponent component: service.getComponents())
        response.add(new RootServiceComponentResponse(serviceName, component.toString(),
                     getComponentVersion(component.name(), null),
                     getComponentProperties(component.name())));
      }
    return response;
  }

  private String getComponentVersion(String componentName, HostResponse host) {
    RootComponent component = RootComponent.valueOf(componentName);
    String componentVersion;
      
    switch (component) {
      case AMBARI_SERVER:
        componentVersion = ambariMetaInfo.getServerVersion();
        break;
      case AMBARI_AGENT:
        //Could be defined on level of host component
        
        if (host == null)
          componentVersion = NOT_APPLICABLE;
        else
          componentVersion = host.getAgentVersion().getVersion();
        break;

      default:
        componentVersion = null;
      }
      
    return componentVersion;
  }
  
  private Map<String, String> getComponentProperties(String componentName){
    
    Map<String, String> response;
    Set<String> propertiesToHideInResponse;
    RootComponent component = null;

    if (componentName != null) {
      component = RootComponent.valueOf(componentName);
      
      switch (component) {
      case AMBARI_SERVER:
        response = configs.getAmbariProperties();
        response.put(JDK_LOCATION, managementController.getJdkResourceUrl());
        response.put("java.version", System.getProperty("java.specification.version"));
        propertiesToHideInResponse = configs.getPropertiesToBlackList();
        for(String key : propertiesToHideInResponse) {
      	  response.remove(key);
      	}
        break;

      default:
        response = Collections.emptyMap();
      }
    }
    else
      response = Collections.emptyMap();

    return response;
  }

  @Override
  public Set<RootServiceHostComponentResponse> getRootServiceHostComponent(RootServiceHostComponentRequest request, Set<HostResponse> hosts) throws AmbariException {
    Set<RootServiceHostComponentResponse> response = new HashSet<>();

    String serviceName = request.getServiceName();
    Set<RootServiceComponentResponse> rootServiceComponents =
        getRootServiceComponents(new RootServiceComponentRequest(serviceName, request.getComponentName()));

    //Cartesian product with hosts and components
    for (RootServiceComponentResponse component : rootServiceComponents) {
      
      Set<HostResponse> filteredHosts = new HashSet<>(hosts);
      
      //Make some filtering of hosts if need
      if (component.getComponentName().equals(RootComponent.AMBARI_SERVER.name())) {
        CollectionUtils.filter(filteredHosts, new Predicate() {
          @Override
          public boolean evaluate(Object arg0) {
            HostResponse hostResponse = (HostResponse) arg0;
            return hostResponse.getHostname().equals(StageUtils.getHostName());
          }
        });
      }
      
      for (HostResponse host : filteredHosts) {
        String state;
        if (component.getComponentName().equals(RootComponent.AMBARI_SERVER.name())) {
          state = RUNNING_STATE;
        } else {
          state = host.getHostState().toString();
        }
        String componentVersion = getComponentVersion(component.getComponentName(), host);
        response.add(new RootServiceHostComponentResponse(serviceName, host.getHostname(), component.getComponentName(), state, componentVersion, component.getProperties()));
      }
    }
    
    return response;
  }
}
