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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.apache.ambari.server.controller.RepositoryResponse;
import org.apache.ambari.server.orm.entities.RepoDefinitionEntity;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.state.RepositoryInfo;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimaps;

public class RepoUtilTest {

  private static final List<String> OPERATING_SYSTEMS = ImmutableList.of("redhat6", "sles11", "ubuntu12");


  @Test public void testAddServiceReposToOperatingSystemEntities_SimpleCase() {
    List<RepoOsEntity> operatingSystems = new ArrayList<>();
    for (String os: OPERATING_SYSTEMS) {
      RepoDefinitionEntity repo1 = repoEntity("HDP", "HDP-2.3", "http://hdp.org/2.3");
      RepoDefinitionEntity repo2 = repoEntity("HDP-UTILS", "HDP-UTILS-1.1.0", "http://hdp.org/utils/1.1.0");
      operatingSystems.add(osEntity(os, repo1, repo2));
    }
    ListMultimap<String, RepositoryInfo> serviceRepos = serviceRepos(ImmutableList.of("redhat5", "redhat6", "sles11"),
        "MSFT_R", "MSFT_R-8.1", "http://msft.r");

    RepoUtil.addServiceReposToOperatingSystemEntities(operatingSystems, serviceRepos);

    // Verify results. Service repos should be added only to redhat6 and sles11
    for (RepoOsEntity os : operatingSystems) {
      Assert.assertNotSame("Redhat5 should not be added as new operating system.", "redhat5", os.getFamily());
      Optional<RepoDefinitionEntity> msft_r = findRepoEntityById(os.getRepoDefinitionEntities(), "MSFT_R-8.1");
      Assert.assertTrue(
          String.format("Only redhat6 and sles11 should contain the service repo. os: %s, repo: %s", os.getFamily(), msft_r),
          findRepoEntityById(os.getRepoDefinitionEntities(), "MSFT_R-8.1").isPresent() == ImmutableList.of("redhat6", "sles11").contains(os.getFamily()));
    }
  }

  @Test public void testAddServiceReposToOperatingSystemEntities_RepoAlreadExists() {
    List<RepoOsEntity> operatingSystems = new ArrayList<>();
    for (String os: OPERATING_SYSTEMS) {
      RepoDefinitionEntity repo1 = repoEntity("HDP", "HDP-2.3", "http://hdp.org/2.3");
      RepoDefinitionEntity repo2 = repoEntity("HDP-UTILS", "HDP-UTILS-1.1.0", "http://hdp.org/utils/1.1.0");
      RepoDefinitionEntity repo3 = repoEntity("MSFT_R", "MSFT_R-8.1", "http://msft.r.ORIGINAL");
      operatingSystems.add(osEntity(os, repo1, repo2, repo3));
    }
    ListMultimap<String, RepositoryInfo> serviceRepos = serviceRepos(ImmutableList.of("redhat6"),
        "MSFT_R", "MSFT_R-8.2", "http://msft.r.NEW");

    RepoUtil.addServiceReposToOperatingSystemEntities(operatingSystems, serviceRepos);

    // Verify results. Service repo should not be added second time.
    for (RepoOsEntity os : operatingSystems) {
      Optional<RepoDefinitionEntity> msft_r_orig = findRepoEntityById(os.getRepoDefinitionEntities(), "MSFT_R-8.1");
      Optional<RepoDefinitionEntity> msft_r_new = findRepoEntityById(os.getRepoDefinitionEntities(), "MSFT_R-8.2");
      Assert.assertTrue("Original repo is missing", msft_r_orig.isPresent());
      Assert.assertTrue("Service repo with duplicate name should not have been added", !msft_r_new.isPresent());
    }
  }

  @Test public void testGetServiceRepos() {
    List<RepositoryInfo> vdfRepos = Lists.newArrayList(repoInfo("HDP", "HDP-2.3", "redhat6"),
        repoInfo("HDP-UTILS", "HDP-UTILS-1.1.0.20", "redhat6"),
        repoInfo("HDP", "HDP-2.3", "redhat5"),
        repoInfo("HDP-UTILS", "HDP-UTILS-1.1.0.20", "redhat5"));
    List<RepositoryInfo> stackRepos = Lists.newArrayList(vdfRepos);
    stackRepos.add(repoInfo("MSFT_R", "MSFT_R-8.1", "redhat6"));

    ImmutableListMultimap<String, RepositoryInfo> stackReposByOs =
        Multimaps.index(stackRepos, RepositoryInfo.GET_OSTYPE_FUNCTION);

    List<RepositoryInfo> serviceRepos = RepoUtil.getServiceRepos(vdfRepos, stackReposByOs);
    Assert.assertEquals("Expected 1 service repo", 1, serviceRepos.size());
    Assert.assertEquals("Expected MSFT_R service repo", "MSFT_R", serviceRepos.get(0).getRepoName());
  }

  @Test public void testAsRepositoryResponses() {
    List<RepositoryInfo> repos = Lists.newArrayList(repoInfo("HDP", "HDP-2.3", "redhat6"),
        repoInfo("HDP-UTILS", "HDP-UTILS-1.1.0.20", "redhat6"),
        repoInfo("HDP", "HDP-2.3", "redhat5"),
        repoInfo("HDP-UTILS", "HDP-UTILS-1.1.0.20", "redhat5"));
    List<RepositoryResponse> responses = RepoUtil.asResponses(repos, "HDP-2.3", "HDP", "2.3");

    Assert.assertEquals("Wrong number of responses", repos.size(), responses.size());
    for (RepositoryResponse response: responses) {
      Assert.assertEquals("Unexpected version definition id", "HDP-2.3", response.getVersionDefinitionId());
      Assert.assertEquals("Unexpected stack name", "HDP", response.getStackName());
      Assert.assertEquals("Unexpected stack version", "2.3", response.getStackVersion());
    }
  }

  private static Optional<RepoDefinitionEntity> findRepoEntityById(Iterable<RepoDefinitionEntity> repos, String repoId) {
    for (RepoDefinitionEntity repo : repos)
      if (Objects.equals(repo.getRepoID(), repoId)) {
      return Optional.of(repo);
    }
    return Optional.absent();
  }

  private static RepoOsEntity osEntity(String os, RepoDefinitionEntity... repoEntities) {
    RepoOsEntity entity = new RepoOsEntity();
    entity.setFamily(os);
    for (RepoDefinitionEntity repo : repoEntities) {
      entity.addRepoDefinition(repo);
    }
    return entity;
  }

  private static RepoDefinitionEntity repoEntity(String name, String repoId, String baseUrl) {
    RepoDefinitionEntity repo = new RepoDefinitionEntity();
    repo.setRepoName(name);
    repo.setRepoID(repoId);
    repo.setBaseUrl(baseUrl);
    return repo;
  }

  private static RepositoryInfo repoInfo(String name, String repoId, String osType) {
    RepositoryInfo repo = new RepositoryInfo();
    repo.setRepoName(name);
    repo.setRepoId(repoId);
    repo.setOsType(osType);
    return repo;
  }

  private static ListMultimap<String, RepositoryInfo> serviceRepos(List<String> operatingSystems,
      String repoName, String repoId, String baseUrl) {
    ListMultimap<String, RepositoryInfo> multimap = ArrayListMultimap.create();
    for (String os: operatingSystems) {
      RepositoryInfo repoInfo = new RepositoryInfo();
      repoInfo.setOsType(os);
      repoInfo.setRepoId(repoId);
      repoInfo.setRepoName(repoName);
      repoInfo.setBaseUrl(baseUrl);
      multimap.put(os, repoInfo);
    }
    return multimap;
  }
}
