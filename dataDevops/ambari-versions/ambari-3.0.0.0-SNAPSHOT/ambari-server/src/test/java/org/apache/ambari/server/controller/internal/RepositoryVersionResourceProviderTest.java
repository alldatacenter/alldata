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

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.controller.ResourceProviderFactory;
import org.apache.ambari.server.controller.predicate.AndPredicate;
import org.apache.ambari.server.controller.spi.Predicate;
import org.apache.ambari.server.controller.spi.Request;
import org.apache.ambari.server.controller.spi.ResourceProvider;
import org.apache.ambari.server.controller.utilities.PredicateBuilder;
import org.apache.ambari.server.controller.utilities.PropertyHelper;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.dao.StackDAO;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.security.TestAuthenticationFactory;
import org.apache.ambari.server.security.authorization.AuthorizationException;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.OperatingSystemInfo;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Injector;
/**
 * RepositoryVersionResourceProvider tests.
 */
public class RepositoryVersionResourceProviderTest {

  private static Injector injector;

  public static final List<RepoOsEntity> osRedhat6 = new ArrayList<>();

  static {
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat6");
    repoOsEntity.setAmbariManaged(true);
    osRedhat6.add(repoOsEntity);
  }

  public static final List<RepoOsEntity> osRedhat7 = new ArrayList<>();

  @Before
  public void before() throws Exception {
    final Set<String> validVersions = Sets.newHashSet("1.1", "1.1-17", "1.1.1.1", "1.1.343432.2", "1.1.343432.2-234234324");
    final Set<StackInfo> stacks = new HashSet<>();

    final AmbariMetaInfo ambariMetaInfo = Mockito.mock(AmbariMetaInfo.class);

    final InMemoryDefaultTestModule injectorModule = new InMemoryDefaultTestModule() {
      @Override
      protected void configure() {
        super.configure();
        bind(AmbariMetaInfo.class).toInstance(ambariMetaInfo);
      };
    };
    injector = Guice.createInjector(injectorModule);

    final StackInfo stackInfo = new StackInfo() {
      @Override
      public Map<String, UpgradePack> getUpgradePacks() {
        final Map<String, UpgradePack> map = new HashMap<>();
        final UpgradePack pack1 = new UpgradePack() {
          @Override
          public String getName() {
            return "pack1";
          }

          @Override
          public String getTarget() {
            return "1.1.*.*";
          }
        };
        final UpgradePack pack2 = new UpgradePack() {
          @Override
          public String getName() {
            return "pack2";
          }

          @Override
          public String getTarget() {
            return "1.1.*.*";
          }
        };
        map.put("pack1", pack1);
        map.put("pack2", pack2);
        return map;
      }

      @Override
      public ServiceInfo getService(String name) {
        return new ServiceInfo();
      }

    };
    stackInfo.setName("HDP");
    stackInfo.setVersion("1.1");
    stacks.add(stackInfo);

    Mockito.when(ambariMetaInfo.getStack(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<StackInfo>() {
      @Override
      public StackInfo answer(InvocationOnMock invocation) throws Throwable {
        final String stack = invocation.getArguments()[0].toString();
        final String version = invocation.getArguments()[1].toString();
        if (stack.equals("HDP") && validVersions.contains(version)) {
          return stackInfo;
        } else {
          throw new Exception("error");
        }
      }

    });

    Mockito.when(ambariMetaInfo.getStacks()).thenReturn(stacks);
    Mockito.when(ambariMetaInfo.getUpgradePacks(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Map<String, UpgradePack>>() {

      @Override
      public Map<String, UpgradePack> answer(InvocationOnMock invocation)
          throws Throwable {
        return stackInfo.getUpgradePacks();
      }

    });

    final HashSet<OperatingSystemInfo> osInfos = new HashSet<>();
    osInfos.add(new OperatingSystemInfo("redhat6"));
    osInfos.add(new OperatingSystemInfo("redhat7"));
    Mockito.when(ambariMetaInfo.getOperatingSystems(Mockito.anyString(), Mockito.anyString())).thenAnswer(new Answer<Set<OperatingSystemInfo>>() {

      @Override
      public Set<OperatingSystemInfo> answer(InvocationOnMock invocation)
          throws Throwable {
        final String stack = invocation.getArguments()[0].toString();
        final String version = invocation.getArguments()[1].toString();
        if (stack.equals("HDP") && validVersions.contains(version)) {
          return osInfos;
        } else {
          return new HashSet<>();
        }
      }
    });

    H2DatabaseCleaner.resetSequences(injector);
    injector.getInstance(GuiceJpaInitializer.class);

    // because AmbariMetaInfo is mocked, the stacks are never inserted into
    // the database, so insert HDP-1.1 manually
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("1.1");
    stackDAO.create(stackEntity);

    Clusters clusters = injector.getInstance(Clusters.class);
    clusters.addCluster("c1", new StackId("HDP", "1.1"));
  }

  @Test
  public void testCreateResourcesAsAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testCreateResourcesAsClusterAdministrator() throws Exception {
    testCreateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  private void testCreateResources(Authentication authentication) throws Exception {
    SecurityContextHolder.getContext().setAuthentication(authentication);

    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionResourceProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<>();
    final Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID, new Gson().fromJson("[{\"OperatingSystems/os_type\":\"redhat6\",\"repositories\":[{\"Repositories/repo_id\":\"1\",\"Repositories/repo_name\":\"1\",\"Repositories/base_url\":\"1\",\"Repositories/unique\":\"true\"}]}]", Object.class));
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, "HDP");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, "1.1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "1.1.1.1");
    propertySet.add(properties);

    final Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    final Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();
    final Request getRequest = PropertyHelper.getReadRequest(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, null);
    provider.createResources(createRequest);

    Assert.assertEquals(1, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());
  }

  @Test
  public void testGetResourcesAsAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test
  public void testGetResourcesAsClusterAdministrator() throws Exception {
    testGetResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  private void testGetResources(Authentication authentication) throws Exception {
    SecurityContextHolder.getContext().setAuthentication(authentication);

    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find("HDP", "1.1");
    Assert.assertNotNull(stackEntity);

    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionResourceProvider();
    final RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    final RepositoryVersionEntity entity = new RepositoryVersionEntity();
    entity.setDisplayName("name");
    entity.addRepoOsEntities(osRedhat6);
    entity.setStack(stackEntity);
    entity.setVersion("1.1.1.1");

    final Request getRequest = PropertyHelper.getReadRequest(RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID,
        RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID);
    final Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    final Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();
    Assert.assertEquals(0, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    repositoryVersionDAO.create(entity);

    Assert.assertEquals(1, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());
  }

  @Test
  public void testValidateRepositoryVersion() throws Exception {
    StackDAO stackDAO = injector.getInstance(StackDAO.class);
    StackEntity stackEntity = stackDAO.find("HDP", "1.1");
    Assert.assertNotNull(stackEntity);

    final RepositoryVersionEntity entity = new RepositoryVersionEntity();
    entity.setDisplayName("name");
    entity.setStack(stackEntity);
    entity.setVersion("1.1");


    List<RepoOsEntity> osEntities = new ArrayList<>();
    RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
    repoDefinitionEntity1.setRepoID("1");
    repoDefinitionEntity1.setBaseUrl("http://example.com/repo1");
    repoDefinitionEntity1.setRepoName("1");
    repoDefinitionEntity1.setUnique(true);
    RepoOsEntity repoOsEntity = new RepoOsEntity();
    repoOsEntity.setFamily("redhat6");
    repoOsEntity.setAmbariManaged(true);
    repoOsEntity.addRepoDefinition(repoDefinitionEntity1);
    osEntities.add(repoOsEntity);

    entity.addRepoOsEntities(osEntities);

    final RepositoryVersionDAO repositoryVersionDAO = injector.getInstance(RepositoryVersionDAO.class);
    AmbariMetaInfo info = injector.getInstance(AmbariMetaInfo.class);

    // test valid usecases
    RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity);
    entity.setVersion("1.1-17");
    RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity);
    entity.setVersion("1.1.1.1");
    RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity);
    entity.setVersion("1.1.343432.2");
    RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity);
    entity.setVersion("1.1.343432.2-234234324");
    RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity);

    // test invalid usecases

    entity.addRepoOsEntities(new ArrayList<>());
    try {
      RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity);
      Assert.fail("Should throw exception");
    } catch (Exception ex) {
    }

    StackEntity bigtop = new StackEntity();
    bigtop.setStackName("BIGTOP");
    entity.setStack(bigtop);
    try {
      RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity);
      Assert.fail("Should throw exception");
    } catch (Exception ex) {
    }


    entity.setDisplayName("name");
    entity.setStack(stackEntity);
    entity.setVersion("1.1");
    entity.addRepoOsEntities(osEntities);
    repositoryVersionDAO.create(entity);

    final RepositoryVersionEntity entity2 = new RepositoryVersionEntity();
    entity2.setId(2l);
    entity2.setDisplayName("name2");
    entity2.setStack(stackEntity);
    entity2.setVersion("1.2");
    entity2.addRepoOsEntities(osEntities);

    try {
      RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity2);
      Assert.fail("Should throw exception: Base url http://example.com/repo1 is already defined for another repository version");
    } catch (Exception ex) {
    }

    final RepositoryVersionEntity entity3 = new RepositoryVersionEntity();
    entity3.setId(3l);
    entity3.setDisplayName("name2");
    entity3.setStack(stackEntity);
    entity3.setVersion("1.1");
    entity3.addRepoOsEntities(osEntities);

    try {
      RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity3);
      Assert.fail("Expected exception");
    } catch (AmbariException e) {
      // expected
    }

    entity3.addRepoOsEntities(osEntities);
    repoOsEntity.setAmbariManaged(false);
    RepositoryVersionResourceProvider.validateRepositoryVersion(repositoryVersionDAO, info, entity3);

  }
  
  @Test
  public void testDeleteResourcesAsAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testDeleteResourcesAsClusterAdministrator() throws Exception {
    testDeleteResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  private void testDeleteResources(Authentication authentication) throws Exception {
    SecurityContextHolder.getContext().setAuthentication(authentication);

    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionResourceProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<>();
    final Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID, new Gson().fromJson("[{\"OperatingSystems/os_type\":\"redhat6\",\"repositories\":[{\"Repositories/repo_id\":\"1\",\"Repositories/repo_name\":\"1\",\"Repositories/base_url\":\"1\",\"Repositories/unique\":\"true\"}]}]", Object.class));
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, "HDP");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, "1.1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "1.1.1.2");
    propertySet.add(properties);

    final Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    final Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();
    final Request getRequest = PropertyHelper.getReadRequest(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, null);
    provider.createResources(createRequest);

    Assert.assertEquals(1, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    final Predicate predicate = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID).equals("1").toPredicate();
    provider.deleteResources(new RequestImpl(null, null, null, null), predicate);

    Assert.assertEquals(0, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());
  }

  @Test
  public void testUpdateResourcesAsAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createAdministrator());
  }

  @Test(expected = AuthorizationException.class)
  public void testUpdateResourcesAsClusterAdministrator() throws Exception {
    testUpdateResources(TestAuthenticationFactory.createClusterAdministrator());
  }

  private void testUpdateResources(Authentication authentication) throws Exception {
    SecurityContextHolder.getContext().setAuthentication(authentication);

    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionResourceProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<>();
    final Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID, new Gson().fromJson("[{\"OperatingSystems/os_type\":\"redhat6\",\"repositories\":[{\"Repositories/repo_id\":\"1\",\"Repositories/repo_name\":\"1\",\"Repositories/base_url\":\"http://example.com/repo1\",\"Repositories/unique\":\"true\"}]}]", Object.class));
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, "HDP");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, "1.1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "1.1.1.1");
    propertySet.add(properties);

    final Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    final Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();
    final Request getRequest = PropertyHelper.getReadRequest(
      RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
      RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, null);
    provider.createResources(createRequest);

    Assert.assertEquals(1, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());
    Assert.assertEquals("name", provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID));

    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID, "1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name2");
    final Request updateRequest = PropertyHelper.getUpdateRequest(properties, null);
    provider.updateResources(updateRequest, new AndPredicate(predicateStackName, predicateStackVersion));

    Assert.assertEquals("name2", provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID));

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    String stackName = properties.get(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString();
    String stackVersion = properties.get(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString();
    Object operatingSystems = properties.get(RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID);
    Gson gson = new Gson();
    String operatingSystemsJson = gson.toJson(operatingSystems);
    RepositoryVersionHelper repositoryVersionHelper = new RepositoryVersionHelper();
    List<RepoOsEntity> operatingSystemEntities = repositoryVersionHelper.parseOperatingSystems(operatingSystemsJson);
    for (RepoOsEntity operatingSystemEntity : operatingSystemEntities) {
      String osType = operatingSystemEntity.getFamily();
      List<RepoDefinitionEntity> repositories = operatingSystemEntity.getRepoDefinitionEntities();
      for (RepoDefinitionEntity repository : repositories) {
        RepositoryInfo repo = ambariMetaInfo.getRepository(stackName, stackVersion, osType, repository.getRepoID());
        if (repo != null) {
          String baseUrlActual = repo.getBaseUrl();
          String baseUrlExpected = repository.getBaseUrl();
          Assert.assertEquals(baseUrlExpected, baseUrlActual);
        }
      }
    }

    properties.put(RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID, new Gson().fromJson("[{\"OperatingSystems/os_type\":\"redhat6\",\"repositories\":[{\"Repositories/repo_id\":\"2\",\"Repositories/repo_name\":\"2\",\"Repositories/base_url\":\"2\",\"Repositories/unique\":\"true\"}]}]", Object.class));
    provider.updateResources(updateRequest, new AndPredicate(predicateStackName, predicateStackVersion));

    try {
      provider.updateResources(updateRequest, new AndPredicate(predicateStackName, predicateStackVersion));
    } catch (Exception ex) {
      Assert.fail("Update of repository should be allowed when repo version is installed on any cluster");
    }
  }

  @Test
  public void testUpdateResourcesNoManageRepos() throws Exception {
    Authentication authentication = TestAuthenticationFactory.createAdministrator();

    SecurityContextHolder.getContext().setAuthentication(authentication);

    final ResourceProvider provider = injector.getInstance(ResourceProviderFactory.class).getRepositoryVersionResourceProvider();

    final Set<Map<String, Object>> propertySet = new LinkedHashSet<>();
    final Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name");
    properties.put(RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID, new Gson().fromJson("[{\"OperatingSystems/os_type\":\"redhat6\",\"repositories\":[{\"Repositories/repo_id\":\"1\",\"Repositories/repo_name\":\"1\",\"Repositories/base_url\":\"http://example.com/repo1\",\"Repositories/unique\":\"true\"}]}]", Object.class));
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID, "HDP");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID, "1.1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_REPOSITORY_VERSION_PROPERTY_ID, "1.1.1.1");
    propertySet.add(properties);

    final Predicate predicateStackName = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).equals("HDP").toPredicate();
    final Predicate predicateStackVersion = new PredicateBuilder().property(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).equals("1.1").toPredicate();
    final Request getRequest = PropertyHelper.getReadRequest(
      RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID,
      RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID);
    Assert.assertEquals(0, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());

    final Request createRequest = PropertyHelper.getCreateRequest(propertySet, null);
    provider.createResources(createRequest);

    Assert.assertEquals(1, provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).size());
    Assert.assertEquals("name", provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID));

    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_ID_PROPERTY_ID, "1");
    properties.put(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID, "name2");
    properties.put(RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID, new Gson().fromJson("[{\"OperatingSystems/ambari_managed_repositories\":false, \"OperatingSystems/os_type\":\"redhat6\",\"repositories\":[{\"Repositories/repo_id\":\"1\",\"Repositories/repo_name\":\"1\",\"Repositories/base_url\":\"http://example.com/repo1\",\"Repositories/unique\":\"true\"}]}]", Object.class));
    final Request updateRequest = PropertyHelper.getUpdateRequest(properties, null);
    provider.updateResources(updateRequest, new AndPredicate(predicateStackName, predicateStackVersion));

    Assert.assertEquals("name2", provider.getResources(getRequest, new AndPredicate(predicateStackName, predicateStackVersion)).iterator().next().getPropertyValue(RepositoryVersionResourceProvider.REPOSITORY_VERSION_DISPLAY_NAME_PROPERTY_ID));

    AmbariMetaInfo ambariMetaInfo = injector.getInstance(AmbariMetaInfo.class);
    String stackName = properties.get(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_NAME_PROPERTY_ID).toString();
    String stackVersion = properties.get(RepositoryVersionResourceProvider.REPOSITORY_VERSION_STACK_VERSION_PROPERTY_ID).toString();
    Object operatingSystems = properties.get(RepositoryVersionResourceProvider.SUBRESOURCE_OPERATING_SYSTEMS_PROPERTY_ID);
    Gson gson = new Gson();
    String operatingSystemsJson = gson.toJson(operatingSystems);
    RepositoryVersionHelper repositoryVersionHelper = new RepositoryVersionHelper();
    List<RepoOsEntity> operatingSystemEntities = repositoryVersionHelper.parseOperatingSystems(operatingSystemsJson);
    for (RepoOsEntity operatingSystemEntity : operatingSystemEntities) {
      Assert.assertFalse(operatingSystemEntity.isAmbariManaged());
      String osType = operatingSystemEntity.getFamily();
      List<RepoDefinitionEntity> repositories = operatingSystemEntity.getRepoDefinitionEntities();
      for (RepoDefinitionEntity repository : repositories) {
        RepositoryInfo repo = ambariMetaInfo.getRepository(stackName, stackVersion, osType, repository.getRepoID());
        if (repo != null) {
          String baseUrlActual = repo.getBaseUrl();
          String baseUrlExpected = repository.getBaseUrl();
          Assert.assertEquals(baseUrlExpected, baseUrlActual);
        }
      }
    }

  }

  @Test
  public void testVersionInStack(){
    StackId sid = new StackId("HDP-2.3");
    StackId sid2 = new StackId("HDP-2.3.NEW");
    StackId sid3 = new StackId("HDF-2.3");

    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid, "2.3"));
    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid2, "2.3"));
    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid3, "2.3"));

    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid, "2.3.1"));
    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid2, "2.3.1"));
    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid3, "2.3.1"));

    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid, "2.3.2.0-2300"));
    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid2, "2.3.2.1-3562"));
    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid3, "2.3.2.1-3562"));

    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid, "HDP-2.3.2.0-2300"));
    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid2, "HDP-2.3.2.1-3562"));
    Assert.assertEquals(true, RepositoryVersionEntity.isVersionInStack(sid3, "HDF-2.3.2.1-3562"));

    Assert.assertEquals(false, RepositoryVersionEntity.isVersionInStack(sid, "2.4.2.0-2300"));
    Assert.assertEquals(false, RepositoryVersionEntity.isVersionInStack(sid2, "2.1"));
    Assert.assertEquals(false, RepositoryVersionEntity.isVersionInStack(sid3, "2.1"));

    Assert.assertEquals(false, RepositoryVersionEntity.isVersionInStack(sid, "HDP-2.4.2.0-2300"));
    Assert.assertEquals(false, RepositoryVersionEntity.isVersionInStack(sid2, "HDP-2.1"));
    Assert.assertEquals(false, RepositoryVersionEntity.isVersionInStack(sid3, "HDF-2.1"));
  }


  @After
  public void after() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
    injector = null;

    SecurityContextHolder.getContext().setAuthentication(null);
  }
}
