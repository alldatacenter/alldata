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
package org.apache.ambari.server.stack;


import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.ClusterDAO;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.ClusterEntity;
import org.apache.ambari.server.orm.entities.ClusterServiceEntity;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceDesiredStateEntity;
import org.apache.ambari.server.orm.entities.StackEntity;
import org.apache.ambari.server.stack.upgrade.RepositoryVersionHelper;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackInfo;
import org.junit.Test;
import org.mockito.Mockito;

import com.google.common.base.Charsets;
import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.gson.Gson;
import com.google.inject.Guice;
import com.google.inject.Provider;

/**
 * Unit test for {@link UpdateActiveRepoVersionOnStartup}
 */
public class UpdateActiveRepoVersionOnStartupTest {

  private static String CLUSTER_NAME = "c1";
  private static String ADD_ON_REPO_ID = "MSFT_R-8.0";

  private RepositoryVersionDAO repositoryVersionDao;
  private UpdateActiveRepoVersionOnStartup activeRepoUpdater;

  @Test
  public void addAServiceRepoToExistingRepoVersion() throws Exception {
    init(true);
    activeRepoUpdater.process();
    verifyRepoIsAdded();
  }

  @Test
  public void missingClusterVersionShouldNotCauseException() throws Exception {
    init(false);
    activeRepoUpdater.process();
  }

  /**
   * Verifies if the add-on service repo is added to the repo version entity, both json and xml representations.
   *
   * @throws Exception
   */
  private void verifyRepoIsAdded() throws Exception {
    verify(repositoryVersionDao, atLeast(1)).merge(Mockito.any(RepositoryVersionEntity.class));
  }

  public void init(boolean addClusterVersion) throws Exception {
    ClusterDAO clusterDao = mock(ClusterDAO.class);

    repositoryVersionDao = mock(RepositoryVersionDAO.class);

    final RepositoryVersionHelper repositoryVersionHelper = new RepositoryVersionHelper();
    Field field = RepositoryVersionHelper.class.getDeclaredField("gson");
    field.setAccessible(true);
    field.set(repositoryVersionHelper, new Gson());

    final AmbariMetaInfo metaInfo = mock(AmbariMetaInfo.class);

    StackManager stackManager = mock(StackManager.class);
    when(metaInfo.getStackManager()).thenReturn(stackManager);

    ClusterEntity cluster = new ClusterEntity();
    cluster.setClusterName(CLUSTER_NAME);
    when(clusterDao.findAll()).thenReturn(ImmutableList.of(cluster));

    StackEntity stackEntity = new StackEntity();
    stackEntity.setStackName("HDP");
    stackEntity.setStackVersion("2.3");
    cluster.setDesiredStack(stackEntity);

    RepositoryVersionEntity desiredRepositoryVersion = new RepositoryVersionEntity();
    desiredRepositoryVersion.setStack(stackEntity);

    List<RepoOsEntity> operatingSystems = new ArrayList<>();
    RepoDefinitionEntity repoDefinitionEntity1 = new RepoDefinitionEntity();
    repoDefinitionEntity1.setRepoID("HDP-UTILS-1.1.0.20");
    repoDefinitionEntity1.setBaseUrl("http://192.168.99.100/repos/HDP-UTILS-1.1.0.20/");
    repoDefinitionEntity1.setRepoName("HDP-UTILS");
    RepoDefinitionEntity repoDefinitionEntity2 = new RepoDefinitionEntity();
    repoDefinitionEntity2.setRepoID("HDP-2.4");
    repoDefinitionEntity2.setBaseUrl("http://192.168.99.100/repos/HDP-2.4.0.0/");
    repoDefinitionEntity2.setRepoName("HDP");
    RepoOsEntity repoOsEntity1 = new RepoOsEntity();
    repoOsEntity1.setFamily("redhat6");
    repoOsEntity1.setAmbariManaged(true);
    repoOsEntity1.addRepoDefinition(repoDefinitionEntity1);
    repoOsEntity1.addRepoDefinition(repoDefinitionEntity2);
    operatingSystems.add(repoOsEntity1);
    RepoDefinitionEntity repoDefinitionEntity3 = new RepoDefinitionEntity();
    repoDefinitionEntity3.setRepoID("HDP-UTILS-1.1.0.20");
    repoDefinitionEntity3.setBaseUrl("http://s3.amazonaws.com/dev.hortonworks.com/HDP-UTILS-1.1.0.20/repos/centos7");
    repoDefinitionEntity3.setRepoName("HDP-UTILS");
    RepoDefinitionEntity repoDefinitionEntity4 = new RepoDefinitionEntity();
    repoDefinitionEntity4.setRepoID("HDP-2.4");
    repoDefinitionEntity4.setBaseUrl("http://s3.amazonaws.com/dev.hortonworks.com/HDP/centos7/2.x/BUILDS/2.4.3.0-207");
    repoDefinitionEntity4.setRepoName("HDP");
    RepoOsEntity repoOsEntity2 = new RepoOsEntity();
    repoOsEntity2.setFamily("redhat7");
    repoOsEntity2.setAmbariManaged(true);
    repoOsEntity2.addRepoDefinition(repoDefinitionEntity3);
    repoOsEntity2.addRepoDefinition(repoDefinitionEntity4);
    operatingSystems.add(repoOsEntity2);

    desiredRepositoryVersion.addRepoOsEntities(operatingSystems);

    ServiceDesiredStateEntity serviceDesiredStateEntity = new ServiceDesiredStateEntity();
    serviceDesiredStateEntity.setDesiredRepositoryVersion(desiredRepositoryVersion);

    ClusterServiceEntity clusterServiceEntity = new ClusterServiceEntity();
    clusterServiceEntity.setServiceDesiredStateEntity(serviceDesiredStateEntity);
    cluster.setClusterServiceEntities(Collections.singletonList(clusterServiceEntity));

    StackInfo stackInfo = new StackInfo();
    stackInfo.setName("HDP");
    stackInfo.setVersion("2.3");

    RepositoryInfo repositoryInfo = new RepositoryInfo();
    repositoryInfo.setBaseUrl("http://msft.r");
    repositoryInfo.setRepoId(ADD_ON_REPO_ID);
    repositoryInfo.setRepoName("MSFT_R");
    repositoryInfo.setOsType("redhat6");
    stackInfo.getRepositories().add(repositoryInfo);

    when(stackManager.getStack("HDP", "2.3")).thenReturn(stackInfo);

    final Provider<RepositoryVersionHelper> repositoryVersionHelperProvider = mock(Provider.class);
    when(repositoryVersionHelperProvider.get()).thenReturn(repositoryVersionHelper);


    InMemoryDefaultTestModule testModule = new InMemoryDefaultTestModule() {
      @Override
      protected void configure() {
        bind(RepositoryVersionHelper.class).toProvider(repositoryVersionHelperProvider);
        bind(AmbariMetaInfo.class).toProvider(new Provider<AmbariMetaInfo>() {
          @Override
          public AmbariMetaInfo get() {
            return metaInfo;
          }
        });

        requestStaticInjection(RepositoryVersionEntity.class);
      }
    };

    Guice.createInjector(testModule);
    if (addClusterVersion) {

      RepositoryInfo info = new RepositoryInfo();
      info.setBaseUrl("http://msft.r");
      info.setRepoId(ADD_ON_REPO_ID);
      info.setRepoName("MSFT_R1");
      info.setOsType("redhat6");
      stackInfo.getRepositories().add(info);
    }

    activeRepoUpdater = new UpdateActiveRepoVersionOnStartup(clusterDao,
        repositoryVersionDao, repositoryVersionHelper, metaInfo);
  }

  private static String resourceAsString(String resourceName) throws IOException {
    return Resources.toString(Resources.getResource(resourceName), Charsets.UTF_8);
  }

}
