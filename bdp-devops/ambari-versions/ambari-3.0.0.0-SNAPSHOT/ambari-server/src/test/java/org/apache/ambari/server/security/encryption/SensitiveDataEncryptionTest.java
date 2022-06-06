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
package org.apache.ambari.server.security.encryption;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.orm.DBAccessor;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Config;
import org.apache.ambari.server.state.stack.OsFamily;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.persist.PersistService;

public class SensitiveDataEncryptionTest {


  @Rule
  public TemporaryFolder tmpFolder = new TemporaryFolder();

  @Before
  public void setUp() throws Exception {
    tmpFolder.create();
  }


  @Test
  public void testSensitiveDataEncryption() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final ConfigPropertiesEncryptor mockConfigPropertiesEncryptor = easyMockSupport.createNiceMock(ConfigPropertiesEncryptor.class);
    final Injector mockInjector = createInjector(mockDBDbAccessor, mockStackManagerFactory,
        mockEntityManager, mockClusters, mockAmbariManagementController, mockOSFamily, mockConfigPropertiesEncryptor);

    Map<String, Cluster> clusters = new HashMap<>();
    Cluster cluster = easyMockSupport.createStrictMock(Cluster.class);
    clusters.put("c1", cluster);
    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(clusters).once();

    List<Config> configs = new ArrayList<Config>();
    Config config = easyMockSupport.createStrictMock(Config.class);
    configs.add(config);
    expect(cluster.getAllConfigs()).andReturn(configs).once();

    mockConfigPropertiesEncryptor.encryptSensitiveData(config);

    config.save();
    expectLastCall();

    final PersistService mockPersistService = easyMockSupport.createNiceMock(PersistService.class);
    SensitiveDataEncryption sensitiveDataEncryption = new SensitiveDataEncryption(mockInjector, mockPersistService);

    easyMockSupport.replayAll();

    sensitiveDataEncryption.doEncryption(true);

    easyMockSupport.verifyAll();
  }


  @Test
  public void testSensitiveDataDecryption() throws Exception {
    EasyMockSupport easyMockSupport = new EasyMockSupport();

    final DBAccessor mockDBDbAccessor = easyMockSupport.createNiceMock(DBAccessor.class);

    final StackManagerFactory mockStackManagerFactory = easyMockSupport.createNiceMock(StackManagerFactory.class);
    final EntityManager mockEntityManager = easyMockSupport.createNiceMock(EntityManager.class);
    final Clusters mockClusters = easyMockSupport.createNiceMock(Clusters.class);
    final AmbariManagementController mockAmbariManagementController = easyMockSupport.createNiceMock(AmbariManagementController.class);
    final OsFamily mockOSFamily = easyMockSupport.createNiceMock(OsFamily.class);
    final ConfigPropertiesEncryptor mockConfigPropertiesEncryptor = easyMockSupport.createNiceMock(ConfigPropertiesEncryptor.class);
    final Injector mockInjector = createInjector(mockDBDbAccessor, mockStackManagerFactory,
        mockEntityManager, mockClusters, mockAmbariManagementController, mockOSFamily, mockConfigPropertiesEncryptor);

    Map<String, Cluster> clusters = new HashMap<>();
    Cluster cluster = easyMockSupport.createStrictMock(Cluster.class);
    clusters.put("c1", cluster);
    expect(mockAmbariManagementController.getClusters()).andReturn(mockClusters).once();
    expect(mockClusters.getClusters()).andReturn(clusters).once();

    List<Config> configs = new ArrayList<Config>();
    Config config = easyMockSupport.createStrictMock(Config.class);
    configs.add(config);
    expect(cluster.getAllConfigs()).andReturn(configs).once();

    mockConfigPropertiesEncryptor.decryptSensitiveData(config);

    config.save();
    expectLastCall();

    final PersistService mockPersistService = easyMockSupport.createNiceMock(PersistService.class);
    SensitiveDataEncryption sensitiveDataEncryption = new SensitiveDataEncryption(mockInjector, mockPersistService);

    easyMockSupport.replayAll();

    sensitiveDataEncryption.doEncryption(false);

    easyMockSupport.verifyAll();
  }

  private Injector createInjector(DBAccessor mockDBDbAccessor, StackManagerFactory mockStackManagerFactory,
                                  EntityManager mockEntityManager, Clusters mockClusters,
                                  AmbariManagementController mockAmbariManagementController, OsFamily mockOSFamily,
                                  ConfigPropertiesEncryptor mockConfigPropertiesEncryptor) {
    return Guice.createInjector(new AbstractModule() {
      @Override
      protected void configure() {
        bind(StackManagerFactory.class).toInstance(mockStackManagerFactory);
        bind(EntityManager.class).toInstance(mockEntityManager);
        bind(DBAccessor.class).toInstance(mockDBDbAccessor);
        bind(Clusters.class).toInstance(mockClusters);
        bind(OsFamily.class).toInstance(mockOSFamily);
        bind(AmbariManagementController.class).toInstance(mockAmbariManagementController);
        bind(ConfigPropertiesEncryptor.class).toInstance(mockConfigPropertiesEncryptor);
      }
    });
  }

}
