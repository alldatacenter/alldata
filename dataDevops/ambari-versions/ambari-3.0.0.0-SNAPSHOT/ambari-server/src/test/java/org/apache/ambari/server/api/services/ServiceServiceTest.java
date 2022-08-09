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

/**
 * Unit tests for ServiceService.
 */
public class ServiceServiceTest extends BaseServiceTest {

  public List<ServiceTestInvocation> getTestInvocations() throws Exception {
    List<ServiceTestInvocation> listInvocations = new ArrayList<>();

    //getService
    ServiceService service = new TestServiceService("clusterName", "serviceName");
    Method m = service.getClass().getMethod("getService", String.class, HttpHeaders.class, UriInfo.class, String.class);
    Object[] args = new Object[] {null, getHttpHeaders(), getUriInfo(), "serviceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //getServices
    service = new TestServiceService("clusterName", null);
    m = service.getClass().getMethod("getServices", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {null, getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, null));

    //createService
    service = new TestServiceService("clusterName", "serviceName");
    m = service.getClass().getMethod("createService", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    //createServices
    service = new TestServiceService("clusterName", null);
    m = service.getClass().getMethod("createServices", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    //updateServices
    service = new TestServiceService("clusterName", "serviceName");
    m = service.getClass().getMethod("updateService", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //updateServices
    service = new TestServiceService("clusterName", null);
    m = service.getClass().getMethod("updateServices", String.class, HttpHeaders.class, UriInfo.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo()};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //deleteServices
    service = new TestServiceService("clusterName", "serviceName");
    m = service.getClass().getMethod("deleteService", HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {getHttpHeaders(), getUriInfo(), "serviceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, null));

    //createArtifact
    service = new TestServiceService("clusterName", "serviceName", "artifactName");
    m = service.getClass().getMethod("createArtifact", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName", "artifactName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.POST, service, m, args, "body"));

    //getArtifact
    service = new TestServiceService("clusterName", "serviceName", "artifactName");
    m = service.getClass().getMethod("getArtifact", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName", "artifactName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, "body"));

    //getArtifacts
    service = new TestServiceService("clusterName", "serviceName");
    m = service.getClass().getMethod("getArtifacts", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.GET, service, m, args, "body"));

    //updateArtifact
    service = new TestServiceService("clusterName", "serviceName", "artifactName");
    m = service.getClass().getMethod("updateArtifact", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName", "artifactName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //updateArtifacts
    service = new TestServiceService("clusterName", "serviceName");
    m = service.getClass().getMethod("updateArtifacts", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.PUT, service, m, args, "body"));

    //deleteArtifact
    service = new TestServiceService("clusterName", "serviceName", "artifactName");
    m = service.getClass().getMethod("deleteArtifact", String.class, HttpHeaders.class, UriInfo.class, String.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName", "artifactName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, "body"));

    //deleteArtifacts
    service = new TestServiceService("clusterName", "serviceName");
    m = service.getClass().getMethod("deleteArtifacts", String.class, HttpHeaders.class, UriInfo.class, String.class);
    args = new Object[] {"body", getHttpHeaders(), getUriInfo(), "serviceName"};
    listInvocations.add(new ServiceTestInvocation(Request.Type.DELETE, service, m, args, "body"));


    return listInvocations;
  }

  private class TestServiceService extends ServiceService {
    private String m_clusterId;
    private String m_serviceId;
    private String m_artifact_id;

    private TestServiceService(String clusterId, String serviceId) {
      super(clusterId);
      m_clusterId = clusterId;
      m_serviceId = serviceId;
    }

    private TestServiceService(String clusterId, String serviceId, String artifactId) {
      super(clusterId);
      m_clusterId = clusterId;
      m_serviceId = serviceId;
      m_artifact_id = artifactId;
    }

    @Override
    ResourceInstance createServiceResource(String clusterName, String serviceName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_serviceId, serviceName);
      return getTestResource();
    }

    @Override
    ResourceInstance createArtifactResource(String clusterName, String serviceName, String artifactName) {
      assertEquals(m_clusterId, clusterName);
      assertEquals(m_serviceId, serviceName);
      assertEquals(m_artifact_id, artifactName);
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
}
