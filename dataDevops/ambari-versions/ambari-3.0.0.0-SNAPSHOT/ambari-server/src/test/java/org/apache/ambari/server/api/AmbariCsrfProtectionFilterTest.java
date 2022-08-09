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

package org.apache.ambari.server.api;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;

import javax.ws.rs.WebApplicationException;

import org.junit.Test;

import com.sun.jersey.core.header.InBoundHeaders;
import com.sun.jersey.spi.container.ContainerRequest;

public class AmbariCsrfProtectionFilterTest {

  @Test
  public void testGetMethod() {
    AmbariCsrfProtectionFilter filter = new AmbariCsrfProtectionFilter();
    ContainerRequest containerRequest = createMock(ContainerRequest.class);
    expect(containerRequest.getMethod()).andReturn("GET");
    replay(containerRequest);
    assertEquals(containerRequest, filter.filter(containerRequest));
  }

  @Test(expected = WebApplicationException.class)
  public void testPostNoXRequestedBy() {
    AmbariCsrfProtectionFilter filter = new AmbariCsrfProtectionFilter();
    ContainerRequest containerRequest = createMock(ContainerRequest.class);
    InBoundHeaders headers = new InBoundHeaders();
    expect(containerRequest.getMethod()).andReturn("POST");
    expect(containerRequest.getRequestHeaders()).andReturn(headers);
    replay(containerRequest);
    filter.filter(containerRequest);
  }

  @Test
  public void testPostXRequestedBy() {
    AmbariCsrfProtectionFilter filter = new AmbariCsrfProtectionFilter();
    ContainerRequest containerRequest = createMock(ContainerRequest.class);
    InBoundHeaders headers = new InBoundHeaders();
    headers.add("X-Requested-By","anything");
    expect(containerRequest.getMethod()).andReturn("GET");
    expect(containerRequest.getRequestHeaders()).andReturn(headers);
    replay(containerRequest);
    assertEquals(containerRequest, filter.filter(containerRequest));
  }

}
