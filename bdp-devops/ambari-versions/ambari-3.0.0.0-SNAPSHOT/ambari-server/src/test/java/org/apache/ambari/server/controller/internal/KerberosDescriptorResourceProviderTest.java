/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.controller.internal.KerberosDescriptorResourceProvider.KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Map;
import java.util.Set;

import javax.persistence.PersistenceException;

import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.ResourceAlreadyExistsException;
import org.apache.ambari.server.orm.dao.KerberosDescriptorDAO;
import org.apache.ambari.server.orm.entities.KerberosDescriptorEntity;
import org.apache.ambari.server.topology.KerberosDescriptorFactory;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class KerberosDescriptorResourceProviderTest {

  @Rule
  public final EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private KerberosDescriptorDAO kerberosDescriptorDAO;

  private final KerberosDescriptorFactory kerberosDescriptorFactory = new KerberosDescriptorFactory();

  @Mock
  private Request request;

  private KerberosDescriptorResourceProvider kerberosDescriptorResourceProvider;

  @Before
  public void before() {
    reset(request, kerberosDescriptorDAO);
    kerberosDescriptorResourceProvider = new KerberosDescriptorResourceProvider(kerberosDescriptorDAO, kerberosDescriptorFactory, null);
    expect(kerberosDescriptorDAO.findByName(anyObject())).andStubReturn(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsCreateWithoutDescriptorText() throws Exception {
    // GIVEN
    expect(request.getProperties()).andReturn(descriptorNamed("any name")).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(ImmutableMap.of()).anyTimes();
    replay(request);

    // WHEN
    kerberosDescriptorResourceProvider.createResources(request);

    // THEN
    // exception is thrown
  }

  @Test(expected = IllegalArgumentException.class)
  public void rejectsCreateWithoutName() throws Exception {
    // GIVEN
    expect(request.getProperties()).andReturn(ImmutableSet.of()).anyTimes();
    replay(request);

    // WHEN
    kerberosDescriptorResourceProvider.createResources(request);

    // THEN
    // exception is thrown
  }

  @Test
  public void acceptsValidRequest() throws Exception {
    // GIVEN
    String name = "some name", text = "any text";
    Capture<KerberosDescriptorEntity> entityCapture = creatingDescriptor(name, text);
    replay(request, kerberosDescriptorDAO);

    // WHEN
    kerberosDescriptorResourceProvider.createResources(request);

    // THEN
    verifyDescriptorCreated(entityCapture, name, text);
  }

  @Test(expected = ResourceAlreadyExistsException.class)
  public void rejectsDuplicateName() throws Exception {
    String name = "any name";
    descriptorAlreadyExists(name);
    tryingToCreateDescriptor(name, "any text");
    replay(request, kerberosDescriptorDAO);

    kerberosDescriptorResourceProvider.createResources(request);
  }

  @Test
  public void canCreateDescriptorWithDifferentName() throws Exception {
    // GIVEN
    descriptorAlreadyExists("some name");

    String name = "another name", text = "any text";
    Capture<KerberosDescriptorEntity> entityCapture = creatingDescriptor(name, text);

    replay(request, kerberosDescriptorDAO);

    // WHEN
    kerberosDescriptorResourceProvider.createResources(request);

    // THEN
    verifyDescriptorCreated(entityCapture, name, text);
  }

  private void verifyDescriptorCreated(Capture<KerberosDescriptorEntity> entityCapture, String name, String text) {
    assertNotNull(entityCapture.getValue());
    assertEquals(name, entityCapture.getValue().getName());
    assertEquals(text, entityCapture.getValue().getKerberosDescriptorText());
  }

  private void descriptorAlreadyExists(String name) {
    KerberosDescriptorEntity entity = new KerberosDescriptorEntity();
    entity.setName(name);
    expect(kerberosDescriptorDAO.findByName(eq(name))).andReturn(entity).anyTimes();

    kerberosDescriptorDAO.create(eq(entity));
    expectLastCall().andThrow(new PersistenceException()).anyTimes();
  }

  private Capture<KerberosDescriptorEntity> creatingDescriptor(String name, String text) {
    tryingToCreateDescriptor(name, text);

    Capture<KerberosDescriptorEntity> entityCapture = EasyMock.newCapture();
    kerberosDescriptorDAO.create(capture(entityCapture));
    expectLastCall().anyTimes();

    return entityCapture;
  }

  private void tryingToCreateDescriptor(String name, String text) {
    expect(request.getProperties()).andReturn(descriptorNamed(name)).anyTimes();
    expect(request.getRequestInfoProperties()).andReturn(descriptorWithText(text)).anyTimes();
  }

  private static Map<String, String> descriptorWithText(String text) {
    return ImmutableMap.of(Request.REQUEST_INFO_BODY_PROPERTY, text);
  }

  private static Set<Map<String, Object>> descriptorNamed(String name) {
    return ImmutableSet.of(ImmutableMap.of(KERBEROS_DESCRIPTOR_NAME_PROPERTY_ID, name));
  }

}