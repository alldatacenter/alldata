package org.apache.ambari.server.orm.dao;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.reset;

import javax.persistence.EntityManager;

import org.apache.ambari.server.orm.entities.KerberosDescriptorEntity;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.Mock;
import org.easymock.MockType;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.google.inject.Provider;

/**
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
public class KerberosDescriptorDAOTest {

  public static final String TEST_KERBEROS_DESCRIPTOR_ENTITY_NAME = "test-kerberos-descriptor-entity";

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock(type = MockType.STRICT)
  private Provider<EntityManager> entityManagerProvider;

  @Mock(type = MockType.STRICT)
  private EntityManager entityManager;

  @TestSubject
  private KerberosDescriptorDAO kerberosDescriptorDAO = new KerberosDescriptorDAO();

  @Before
  public void before() {
    reset(entityManagerProvider);
    expect(entityManagerProvider.get()).andReturn(entityManager).atLeastOnce();
    replay(entityManagerProvider);
  }


  @Test
  public void testPersistNewKerberosDescriptorEntity() {
    //GIVEN
    KerberosDescriptorEntity kerberosDescriptorEntity = new KerberosDescriptorEntity();
    kerberosDescriptorEntity.setName(TEST_KERBEROS_DESCRIPTOR_ENTITY_NAME);

    Capture<KerberosDescriptorEntity> capturedArgument = EasyMock.newCapture();
    entityManager.persist(capture(capturedArgument));
    replay(entityManager);

    //WHEN
    kerberosDescriptorDAO.create(kerberosDescriptorEntity);

    //THEN
    Assert.assertNotNull(capturedArgument);
    Assert.assertEquals("The persisted object is not the expected one", kerberosDescriptorEntity, capturedArgument.getValue());
  }

}