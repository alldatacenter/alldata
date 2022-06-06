/*
 *
 *  * Licensed to the Apache Software Foundation (ASF) under one
 *  * or more contributor license agreements.  See the NOTICE file
 *  * distributed with this work for additional information
 *  * regarding copyright ownership.  The ASF licenses this file
 *  * to you under the Apache License, Version 2.0 (the
 *  * "License"); you may not use this file except in compliance
 *  * with the License.  You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package org.apache.ambari.server.controller.internal;

import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.LDAP_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.SSO_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationCategory.TPROXY_CONFIGURATION;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.LDAP_ENABLED;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SERVER_HOST;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_ENABLED_SERVICES;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.SSO_MANAGE_SERVICES;
import static org.apache.ambari.server.configuration.AmbariServerConfigurationKey.TPROXY_AUTHENTICATION_ENABLED;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.RootServiceComponentConfiguration;
import org.apache.ambari.server.events.AmbariConfigurationChangedEvent;
import org.apache.ambari.server.events.publishers.AmbariEventPublisher;
import org.apache.ambari.server.orm.dao.AmbariConfigurationDAO;
import org.apache.ambari.server.orm.entities.AmbariConfigurationEntity;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.junit.Test;
import org.junit.runner.RunWith;

import junit.framework.Assert;

@RunWith(EasyMockRunner.class)
public class AmbariServerConfigurationHandlerTest extends EasyMockSupport {

  @Test
  public void getComponentConfigurations() {
    List<AmbariConfigurationEntity> ssoEntities = new ArrayList<>();
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_MANAGE_SERVICES.key(), "true"));
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_ENABLED_SERVICES.key(), "AMBARI,SERVICE1"));

    List<AmbariConfigurationEntity> ldapEntities = new ArrayList<>();
    ldapEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), LDAP_ENABLED.key(), "true"));
    ldapEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), SERVER_HOST.key(), "host1"));

    List<AmbariConfigurationEntity> tproxyEntities = new ArrayList<>();
    tproxyEntities.add(createEntity(TPROXY_CONFIGURATION.getCategoryName(), TPROXY_AUTHENTICATION_ENABLED.key(), "true"));
    tproxyEntities.add(createEntity(TPROXY_CONFIGURATION.getCategoryName(), "ambari.tproxy.proxyuser.knox.hosts", "host1"));

    List<AmbariConfigurationEntity> allEntities = new ArrayList<>();
    allEntities.addAll(ssoEntities);
    allEntities.addAll(ldapEntities);
    allEntities.addAll(tproxyEntities);

    AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
    expect(ambariConfigurationDAO.findAll()).andReturn(allEntities).once();
    expect(ambariConfigurationDAO.findByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(ssoEntities).once();
    expect(ambariConfigurationDAO.findByCategory(LDAP_CONFIGURATION.getCategoryName())).andReturn(ldapEntities).once();
    expect(ambariConfigurationDAO.findByCategory(TPROXY_CONFIGURATION.getCategoryName())).andReturn(tproxyEntities).once();
    expect(ambariConfigurationDAO.findByCategory("invalid category")).andReturn(null).once();

    AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);

    AmbariServerConfigurationHandler handler = new AmbariServerConfigurationHandler(ambariConfigurationDAO, publisher);

    replayAll();

    Map<String, RootServiceComponentConfiguration> allConfigurations = handler.getComponentConfigurations(null);
    Assert.assertEquals(3, allConfigurations.size());
    Assert.assertTrue(allConfigurations.containsKey(SSO_CONFIGURATION.getCategoryName()));
    Assert.assertTrue(allConfigurations.containsKey(LDAP_CONFIGURATION.getCategoryName()));
    Assert.assertTrue(allConfigurations.containsKey(TPROXY_CONFIGURATION.getCategoryName()));

    Map<String, RootServiceComponentConfiguration> ssoConfigurations = handler.getComponentConfigurations(SSO_CONFIGURATION.getCategoryName());
    Assert.assertEquals(1, ssoConfigurations.size());
    Assert.assertTrue(ssoConfigurations.containsKey(SSO_CONFIGURATION.getCategoryName()));

    Map<String, RootServiceComponentConfiguration> ldapConfigurations = handler.getComponentConfigurations(LDAP_CONFIGURATION.getCategoryName());
    Assert.assertEquals(1, ldapConfigurations.size());
    Assert.assertTrue(ldapConfigurations.containsKey(LDAP_CONFIGURATION.getCategoryName()));

    Map<String, RootServiceComponentConfiguration> tproxyConfigurations = handler.getComponentConfigurations(TPROXY_CONFIGURATION.getCategoryName());
    Assert.assertEquals(1, tproxyConfigurations.size());
    Assert.assertTrue(tproxyConfigurations.containsKey(TPROXY_CONFIGURATION.getCategoryName()));

    Map<String, RootServiceComponentConfiguration> invalidConfigurations = handler.getComponentConfigurations("invalid category");
    Assert.assertNull(invalidConfigurations);

    verifyAll();
  }

  @Test
  public void removeComponentConfiguration() {
    AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
    expect(ambariConfigurationDAO.removeByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(1).once();
    expect(ambariConfigurationDAO.removeByCategory("invalid category")).andReturn(0).once();

    AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);
    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().once();

    AmbariServerConfigurationHandler handler = new AmbariServerConfigurationHandler(ambariConfigurationDAO, publisher);

    replayAll();

    handler.removeComponentConfiguration(SSO_CONFIGURATION.getCategoryName());
    handler.removeComponentConfiguration("invalid category");

    verifyAll();
  }

  @Test
  public void updateComponentCategory() throws AmbariException {
    Map<String, String> properties = new HashMap<>();
    properties.put(SSO_ENABLED_SERVICES.key(), "SERVICE1");
    properties.put(SSO_MANAGE_SERVICES.key(), "true");

    AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
    expect(ambariConfigurationDAO.reconcileCategory(SSO_CONFIGURATION.getCategoryName(), properties, true))
        .andReturn(true).once();
    expect(ambariConfigurationDAO.reconcileCategory(SSO_CONFIGURATION.getCategoryName(), properties, false))
        .andReturn(true).once();

    AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);
    publisher.publish(anyObject(AmbariConfigurationChangedEvent.class));
    expectLastCall().times(2);

    AmbariServerConfigurationHandler handler = new AmbariServerConfigurationHandler(ambariConfigurationDAO, publisher);

    replayAll();

    handler.updateComponentCategory(SSO_CONFIGURATION.getCategoryName(), properties, false);

    handler.updateComponentCategory(SSO_CONFIGURATION.getCategoryName(), properties, true);

    try {
      handler.updateComponentCategory("invalid category", properties, true);
      Assert.fail("Expecting IllegalArgumentException to be thrown");
    } catch (IllegalArgumentException e) {
      // This is expected
    }

    verifyAll();
  }

  @Test
  public void getConfigurations() {
    List<AmbariConfigurationEntity> ssoEntities = new ArrayList<>();
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_MANAGE_SERVICES.key(), "true"));
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_ENABLED_SERVICES.key(), "AMBARI,SERVICE1"));

    List<AmbariConfigurationEntity> allEntities = new ArrayList<>(ssoEntities);
    allEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), LDAP_ENABLED.key(), "true"));
    allEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), SERVER_HOST.key(), "host1"));

    AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
    expect(ambariConfigurationDAO.findAll()).andReturn(allEntities).once();

    AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);

    AmbariServerConfigurationHandler handler = new AmbariServerConfigurationHandler(ambariConfigurationDAO, publisher);

    replayAll();

    Map<String, Map<String, String>> allConfigurations = handler.getConfigurations();
    Assert.assertEquals(2, allConfigurations.size());
    Assert.assertTrue(allConfigurations.containsKey(SSO_CONFIGURATION.getCategoryName()));
    Assert.assertTrue(allConfigurations.containsKey(LDAP_CONFIGURATION.getCategoryName()));

    verifyAll();
  }

  @Test
  public void getConfigurationProperties() {
    List<AmbariConfigurationEntity> ssoEntities = new ArrayList<>();
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_MANAGE_SERVICES.key(), "true"));
    ssoEntities.add(createEntity(SSO_CONFIGURATION.getCategoryName(), SSO_ENABLED_SERVICES.key(), "AMBARI,SERVICE1"));

    List<AmbariConfigurationEntity> allEntities = new ArrayList<>(ssoEntities);
    allEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), LDAP_ENABLED.key(), "true"));
    allEntities.add(createEntity(LDAP_CONFIGURATION.getCategoryName(), SERVER_HOST.key(), "host1"));

    AmbariConfigurationDAO ambariConfigurationDAO = createMock(AmbariConfigurationDAO.class);
    expect(ambariConfigurationDAO.findByCategory(SSO_CONFIGURATION.getCategoryName())).andReturn(ssoEntities).once();
    expect(ambariConfigurationDAO.findByCategory("invalid category")).andReturn(null).once();

    AmbariEventPublisher publisher = createMock(AmbariEventPublisher.class);

    AmbariServerConfigurationHandler handler = new AmbariServerConfigurationHandler(ambariConfigurationDAO, publisher);

    replayAll();

    Map<String, String> ssoConfigurations = handler.getConfigurationProperties(SSO_CONFIGURATION.getCategoryName());
    Assert.assertEquals(2, ssoConfigurations.size());
    Assert.assertTrue(ssoConfigurations.containsKey(SSO_ENABLED_SERVICES.key()));
    Assert.assertTrue(ssoConfigurations.containsKey(SSO_MANAGE_SERVICES.key()));

    Map<String, String> invalidConfigurations = handler.getConfigurationProperties("invalid category");
    Assert.assertNull(invalidConfigurations);

    verifyAll();
  }


  private AmbariConfigurationEntity createEntity(String categoryName, String key, String value) {
    AmbariConfigurationEntity entity = new AmbariConfigurationEntity();
    entity.setCategoryName(categoryName);
    entity.setPropertyName(key);
    entity.setPropertyValue(value);
    return entity;
  }
}