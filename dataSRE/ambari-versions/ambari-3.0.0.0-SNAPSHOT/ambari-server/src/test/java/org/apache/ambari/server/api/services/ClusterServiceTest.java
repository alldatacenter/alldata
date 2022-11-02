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

package org.apache.ambari.server.api.services;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.ambari.server.api.resources.ResourceInstance;
import org.apache.ambari.server.api.services.parsers.RequestBodyParser;
import org.apache.ambari.server.api.services.serializers.ResultSerializer;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.HostDAO;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.cluster.ClusterFactory;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.host.HostFactory;
import org.easymock.EasyMock;

/**
 * Unit tests for ClusterService.
 */
public class ClusterServiceTest extends BaseServiceTest {


  @Override
  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    ClusterDAO clusterDAO = EasyMock.createNiceMock(ClusterDAO.class);
    HostDAO hostDAO = EasyMock.createNiceMock(HostDAO.class);

    EasyMock.expect(clusterDAO.findAll()).andReturn(new ArrayList<>()).atLeastOnce();
    EasyMock.expect(hostDAO.findAll()).andReturn(new ArrayList<>()).atLeastOnce();

    EasyMock.replay(clusterDAO, hostDAO);

    Clusters clusters = new TestClusters(clusterDAO, EasyMock.createNiceMock(ClusterFactory.class),
        hostDAO, EasyMock.createNiceMock(HostFactory.class));

    ClusterService clusterService;
    Method m;
    Object[] args;

    //getCluster
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("getCluster", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, clusterService, m, args, null));

    //getClusters
    clusterService = new TestClusterService(clusters, null);
    m = clusterService.getClass().getMethod("getClusters", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, clusterService, m, args, null));

    //createCluster
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("createCluster", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, clusterService, m, args, "body"));

    //updateCluster
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("updateCluster", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, clusterService, m, args, "body"));

    //deleteCluster
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("deleteCluster", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, clusterService, m, args, null));

    //createClusterArtifact
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("createClusterArtifact", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName", "artifactName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, clusterService, m, args, "body"));

    //getClusterArtifact
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("getClusterArtifact", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName", "artifact_name"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, clusterService, m, args, "body"));

    //getClusterArtifacts
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("getClusterArtifacts", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, clusterService, m, args, "body"));

    //updateClusterArtifact
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("updateClusterArtifact", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName", "artifactName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, clusterService, m, args, "body"));

    //updateClusterArtifacts
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("updateClusterArtifacts", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, clusterService, m, args, "body"));

    //deleteClusterArtifact
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("deleteClusterArtifact", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName", "artifactName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, clusterService, m, args, "body"));

    //deleteClusterArtifacts
    clusterService = new TestClusterService(clusters, "clusterName");
    m = clusterService.getClass().getMethod("deleteClusterArtifacts", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "clusterName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, clusterService, m, args, "body"));

    return listInvocations;
  }


  private class TestClusterService extends ClusterService {
    private String m_clusterId;

    private TestClusterService(Clusters clusters, String clusterId) {
      super(clusters);
      m_clusterId = clusterId;
    }

    @Override
    ResourceInstance createClusterResource(String clusterName) {
      assertEquals(m_clusterId, clusterName);
      return getTestResource();
    }

    @Override
    ResourceInstance createArtifactResource(String clusterName, String artifactName) {
      assertEquals(m_clusterId, clusterName);
      return getTestResource();
    }

    @Override
    RequestFactory getRequestFactory() {
      return getTestRequestFactory();
    }

    @Override
    protected RequestBodyParser getBodyParser() {
      return getTestBodyParser();
    }

    @Override
    protected ResultSerializer getResultSerializer() {
      return getTestResultSerializer();
    }
  }

  private class TestClusters extends ClustersImpl {
    public TestClusters(ClusterDAO clusterDAO, ClusterFactory clusterFactory, HostDAO hostDAO,
        HostFactory hostFactory) {

      super(clusterDAO, clusterFactory, hostDAO, hostFactory);
    }

    @Override
    public boolean checkPermission(String clusterName, boolean readOnly) {
      return true;
    }
  }

  //todo: test getHostHandler, getServiceHandler, getHostComponentHandler
}
