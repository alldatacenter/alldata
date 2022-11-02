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


package org.apache.ambari.server.controller.internal;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.ambari.server.controller.AbstractRootServiceResponseFactory;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.controller.HostResponse;
import org.apache.ambari.server.controller.RootServiceHostComponentRequest;
import org.apache.ambari.server.controller.RootServiceHostComponentResponse;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.easymock.EasyMock;
import org.junit.Test;

public class RootServiceHostComponentResourceProviderTest {
    
  @Test
  public void testGetResources() throws Exception{
    Resource.Type type = Resource.Type.RootServiceHostComponent;

    AmbariManagementController managementController = createMock(AmbariManagementController.class);
    Clusters clusters = createNiceMock(Clusters.class);
    Cluster cluster = createNiceMock(Cluster.class);
    Host host1 = createNiceMock(Host.class);

    HostResponse hostResponse1 = createNiceMock(HostResponse.class);

    RootServiceHostComponentResponse response = createNiceMock(RootServiceHostComponentResponse.class);

    AbstractRootServiceResponseFactory factory = createNiceMock(AbstractRootServiceResponseFactory.class);

    List<Host> hosts = new LinkedList<>();
    hosts.add(host1);

    Set<Cluster> clusterSet = new HashSet<>();
    clusterSet.add(cluster);

    Set<RootServiceHostComponentResponse> responseSet = new HashSet<>();
    responseSet.add(response);

    // set expectations
    expect(managementController.getRootServiceResponseFactory()).andReturn(factory).anyTimes();
    expect(managementController.getClusters()).andReturn(clusters).anyTimes();
    expect(clusters.getHosts()).andReturn(hosts).anyTimes();

    expect(factory.getRootServiceHostComponent((RootServiceHostComponentRequest) anyObject(), EasyMock.anyObject())).
        andReturn(responseSet).anyTimes();

    expect(clusters.getCluster("Cluster100")).andReturn(cluster).anyTimes();

    expect(clusters.getClustersForHost("Host100")).andReturn(clusterSet).anyTimes();

    expect(host1.getHostName()).andReturn("Host100").anyTimes();

    expect(host1.convertToResponse()).andReturn(hostResponse1).anyTimes();

    expect(hostResponse1.getClusterName()).andReturn("Cluster100").anyTimes();
    expect(hostResponse1.getHostname()).andReturn("Host100").anyTimes();
    expect(hostResponse1.getHealthReport()).andReturn("HEALTHY").anyTimes();

    // replay
    replay(managementController, clusters, cluster,
        host1,
        hostResponse1,
        factory, response);


    ResourceProvider provider = AbstractControllerResourceProvider.getResourceProvider(
        type,
        managementController);

    Set<String> propertyIds = new HashSet<>();

    propertyIds.add(RootServiceHostComponentResourceProvider.SERVICE_NAME_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.HOST_NAME_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.COMPONENT_NAME_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.COMPONENT_STATE_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.PROPERTIES_PROPERTY_ID);
    propertyIds.add(RootServiceHostComponentResourceProvider.COMPONENT_VERSION_PROPERTY_ID);
    
    // create the request
    Request request = PropertyHelper.getReadRequest(propertyIds);

    // get all ... no predicate
    provider.getResources(request, null);

    // verify
    verify(managementController, clusters, cluster,
        host1,
        hostResponse1,
        factory, response);
  }

}
