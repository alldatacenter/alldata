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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.predicate.OrPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.Resource;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.server.state.repository.AvailableService;
import org.apache.ambari.server.state.repository.ManifestServiceInfo;
import org.apache.ambari.server.state.repository.VersionDefinitionXml;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.inject.Guice;
import com.google.inject.Injector;

/**
 * CompatibleRepositoryVersionResourceProvider tests.
 */
public class CompatibleRepositoryVersionResourceProviderTest {

  private static Injector injector;

  private static final List<RepoOsEntity> osRedhat6 = new ArrayList<>();

  {
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat6");
    repoOsEntity.setAmbariManaged(true);
    osRedhat6.add(repoOsEntity);
  }
  private static StackId stackId11 = new StackId("HDP", "1.1");
  private static StackId stackId22 = new StackId("HDP", "2.2");

  @Before
  public void before() throws Exception {
    final AmbariMetaInfo ambariMetaInfo = EasyMock.createMock(AmbariMetaInfo.class);

    StackEntity hdp11Stack = new StackEntity();
    hdp11Stack.setStackName("HDP");
    hdp11Stack.setStackVersion("1.1");

    RepositoryVersionEntity entity1 = new RepositoryVersionEntity();
    entity1.setDisplayName("name1");
    entity1.addRepoOsEntities(osRedhat6);
    entity1.setStack(hdp11Stack);
    entity1.setVersion("1.1.1.1");
    entity1.setId(1L);

    StackEntity hdp22Stack = new StackEntity();
    hdp22Stack.setStackName("HDP");
    hdp22Stack.setStackVersion("2.2");

    RepositoryVersionEntity entity2 = new ExtendedRepositoryVersionEntity();
    entity2.setDisplayName("name2");
    entity2.addRepoOsEntities(osRedhat6);
    entity2.setStack(hdp22Stack);
    entity2.setVersion("2.2.2.2");
    entity2.setId(2L);

    final RepositoryVersionDAO repoVersionDAO = EasyMock.createMock(RepositoryVersionDAO.class);

    expect(repoVersionDAO.findByStack(stackId11)).andReturn(Collections.singletonList(entity1)).atLeastOnce();
    expect(repoVersionDAO.findByStack(stackId22)).andReturn(Collections.singletonList(entity2)).atLeastOnce();
    replay(repoVersionDAO);

    final StackInfo stack1 = new StackInfo() {
      @Override
      public Map<String, UpgradePack> getUpgradePacks() {
        Map<String, UpgradePack> map = new HashMap<>();

        UpgradePack pack1 = new UpgradePack() {

          @Override
          public String getName() {
            return "pack1";
          }

          @Override
          public String getTarget() {
            return "1.1.*.*";
          }

          @Override
          public UpgradeType getType() {
            return UpgradeType.ROLLING;
          }
        };

        UpgradePack pack2 = new UpgradePack() {
          @Override
          public String getName() {
            return "pack2";
          }

          @Override
          public String getTarget() {
            return "2.2.*.*";
          }

          @Override
          public String getTargetStack() {
            return "HDP-2.2";
          }

          @Override
          public UpgradeType getType() {
            return UpgradeType.NON_ROLLING;
          }
        };

        UpgradePack pack3 = new UpgradePack() {
          @Override
          public String getName() {
            return "pack2";
          }

          @Override
          public String getTarget() {
            return "2.2.*.*";
          }

          @Override
          public String getTargetStack() {
            return "HDP-2.2";
          }

          @Override
          public UpgradeType getType() {
            return UpgradeType.ROLLING;
          }
        };

        map.put("pack1", pack1);
        map.put("pack2", pack2);
        map.put("pack3", pack3);
        return map;
      }
    };

    final StackInfo stack2 = new StackInfo() {
      @Override
      public Map<String, UpgradePack> getUpgradePacks() {
        Map<String, UpgradePack> map = new HashMap<>();

        UpgradePack pack = new UpgradePack() {
          @Override
          public String getName() {
            return "pack2";
          }

          @Override
          public String getTarget() {
            return "2.2.*.*";
          }

          @Override
          public UpgradeType getType() {
            return UpgradeType.NON_ROLLING;
          }
        };

        map.put("pack2", pack);
        return map;
      }
    };


    InMemoryDefaultTestModule injectorModule = new InMemoryDefaultTestModule() {
      @Override
      protected void configure() {
        super.configure();
        bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
        bind(RepositoryVersionDAO.class).toInstance(repoVersionDAO);
        requestStaticInjection(CompatibleRepositoryVersionResourceProvider.class);
      }

      ;
    };
    injector = Guice.createInjector(injectorModule);

    expect(ambariMetaInfo.getStack("HDP", "1.1")).andReturn(stack1).atLeastOnce();
    expect(ambariMetaInfo.getStack("HDP", "2.2")).andReturn(stack2).atLeastOnce();
    expect(ambariMetaInfo.getUpgradePacks("HDP", "1.1")).andReturn(stack1.getUpgradePacks()).atLeastOnce();
    expect(ambariMetaInfo.getUpgradePacks("HDP", "2.2")).andReturn(stack2.getUpgradePacks()).atLeastOnce();

    replay(ambariMetaInfo);

    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;

    SecurityContextHolder.getContext().setAuthentication(null);
  }

  @Test
  public void testVersionInStack() {
    assertTrue(RepositoryVersionEntity.isVersionInStack(new StackId("HDP", "2.3"), "2.3.0.0"));
    assertTrue(RepositoryVersionEntity.isVersionInStack(new StackId("HDPWIN", "2.3"), "2.3.0.0"));
    assertTrue(RepositoryVersionEntity.isVersionInStack(new StackId("HDP", "2.3.GlusterFS"), "2.3.0.0"));
    assertTrue(RepositoryVersionEntity.isVersionInStack(new StackId("HDF", "2.1"), "2.1.0.0"));

    assertTrue(RepositoryVersionEntity.isVersionInStack(new StackId("HDP", "2.3"), "HDP-2.3.0.0"));
    assertTrue(RepositoryVersionEntity.isVersionInStack(new StackId("HDPWIN", "2.3"), "HDPWIN-2.3.0.0"));
    assertTrue(RepositoryVersionEntity.isVersionInStack(new StackId("HDP", "2.3.GlusterFS"), "HDP-2.3.0.0"));
    assertTrue(RepositoryVersionEntity.isVersionInStack(new StackId("HDF", "2.1"), "HDF-2.1.0.0"));
  }

  @Test
  public void testGetResources() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("admin", 2L));

    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionResourceProvider();

    Request getRequest = PropertyHelper.getReadRequest(
      RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
      RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
      RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID);
    Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();

    // !!! non-compatible, within stack
    assertEquals(1, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    CompatibleRepositoryVersionResourceProvider compatibleProvider = new CompatibleRepositoryVersionResourceProvider(null);

    getRequest = PropertyHelper.getReadRequest(
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID);
    predicateStackName = new PredicateBuilder().property(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    predicateStackVersion = new PredicateBuilder().property(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();

    // !!! compatible, across stack
    Set<Resource> resources = compatibleProvider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion));
    assertEquals(2, resources.size());

    // Test For Upgrade Types
    Map<String, List<UpgradeType>> versionToUpgradeTypesMap = new HashMap<>();
    versionToUpgradeTypesMap.put("1.1", Arrays.asList(UpgradeType.ROLLING));
    versionToUpgradeTypesMap.put("2.2", Arrays.asList(UpgradeType.NON_ROLLING, UpgradeType.ROLLING));
    assertEquals(versionToUpgradeTypesMap.size(), checkUpgradeTypes(resources, versionToUpgradeTypesMap));

    // !!! verify we can get services
    RepositoryVersionDAO dao = injector.getInstance(RepositoryVersionDAO.class);
    List<RepositoryVersionEntity> entities = dao.findByStack(stackId22);
    assertEquals(1, entities.size());
    RepositoryVersionEntity entity = entities.get(0);
    assertTrue(ExtendedRepositoryVersionEntity.class.isInstance(entity));

    VersionDefinitionXml mockXml = EasyMock.createMock(VersionDefinitionXml.class);
    AvailableService mockAvailable = EasyMock.createMock(AvailableService.class);
    ManifestServiceInfo mockStackService = EasyMock.createMock(ManifestServiceInfo.class);

    expect(mockXml.getAvailableServices((StackInfo)EasyMock.anyObject())).andReturn(Collections.singletonList(mockAvailable)).atLeastOnce();
    expect(mockXml.getStackServices((StackInfo)EasyMock.anyObject())).andReturn(Collections.singletonList(mockStackService)).atLeastOnce();

    replay(mockXml);

    ((ExtendedRepositoryVersionEntity) entity).m_xml = mockXml;

    getRequest = PropertyHelper.getReadRequest(
        CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
        CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
        CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
        CompatibleRepositoryVersionResourceProvider.REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID,
        CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_SERVICES);
    predicateStackName = new PredicateBuilder().property(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    predicateStackVersion = new PredicateBuilder().property(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();

    resources = compatibleProvider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion));
    assertEquals(2, resources.size());

    for (Resource r : resources) {
      Object stackId = r.getPropertyValue(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
      assertNotNull(stackId);
      if (stackId.toString().equals("2.2")) {
        assertNotNull(r.getPropertyValue(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_SERVICES));
      } else {
        assertNull(r.getPropertyValue(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_SERVICES));
      }
    }
  }

  @Test
  public void testGetResourcesWithAmendedPredicate() throws Exception {
    SecurityContextHolder.getContext().setAuthentication(TestAuthenticationFactory.createClusterAdministrator("admin", 2L));

    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionResourceProvider();

    Request getRequest = PropertyHelper.getReadRequest(
      RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
      RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
      RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID);
    Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();

    // !!! non-compatible, within stack
    assertEquals(1, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    CompatibleRepositoryVersionResourceProvider compatibleProvider = new CompatibleRepositoryVersionResourceProvider(null);

    getRequest = PropertyHelper.getReadRequest(
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID,
      CompatibleRepositoryVersionResourceProvider.REPOSITORY_UPGRADES_SUPPORTED_TYPES_ID);
    predicateStackName = new PredicateBuilder().property(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    predicateStackVersion = new PredicateBuilder().property(CompatibleRepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();

    // !!! compatible, across stack
    Predicate newPredicate = compatibleProvider.amendPredicate(new AndPredicate(predicateStackName, predicateStackVersion));

    assertEquals(OrPredicate.class, newPredicate.getClass());

    Set<Resource> resources = compatibleProvider.getResources(getRequest, newPredicate);
    assertEquals(2, resources.size());

    // Test For Upgrade Types
    Map<String, List<UpgradeType>> versionToUpgradeTypesMap = new HashMap<>();
    versionToUpgradeTypesMap.put("1.1", Arrays.asList(UpgradeType.ROLLING));
    versionToUpgradeTypesMap.put("2.2", Arrays.asList(UpgradeType.NON_ROLLING, UpgradeType.ROLLING));
    assertEquals(versionToUpgradeTypesMap.size(), checkUpgradeTypes(resources, versionToUpgradeTypesMap));
  }

  /**
   * Checks for UpgradeTypes for the specified Target stack versions.
   *
   * @param resources                The resource Set to iterate over
   * @param versionToUpgradeTypesMap Contains 'Stack version' to 'Upgrade Type' Map.
   * @return count, 0 or number of Stack version's Upgrade Type(s) correctly compared.
   */
  public int checkUpgradeTypes(Set<Resource> resources, Map<String, List<UpgradeType>> versionToUpgradeTypesMap) {
    int count = 0;
    Iterator<Resource> itr = resources.iterator();
    while (itr.hasNext()) {
      Resource res = itr.next();
      Map<String, Map<String, Object>> resPropMap = res.getPropertiesMap();
      for (String resource : resPropMap.keySet()) {
        Map<String, Object> propMap = resPropMap.get(resource);
        String stackVersion = propMap.get("stack_version").toString();
        if (versionToUpgradeTypesMap.containsKey(stackVersion)) {
          List<UpgradeType> upgradeTypes = new ArrayList<>((Set<UpgradeType>)propMap.get("upgrade_types"));
          List<UpgradeType> expectedTypes = versionToUpgradeTypesMap.get(stackVersion);
          Collections.sort(upgradeTypes);
          Collections.sort(expectedTypes);

          assertEquals(expectedTypes, upgradeTypes);
          count++;
        }
      }
    }
    return count;
  }

  private static class ExtendedRepositoryVersionEntity extends RepositoryVersionEntity {
    private VersionDefinitionXml m_xml = null;

    @Override
    public VersionDefinitionXml getRepositoryXml() throws Exception {
      return m_xml;
    }

  }

}
