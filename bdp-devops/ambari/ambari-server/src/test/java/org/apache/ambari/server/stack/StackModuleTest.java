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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.ServiceInfo;
import org.apache.ambari.server.state.stack.RepositoryXml;
import org.apache.ambari.server.state.stack.ServiceMetainfoXml;
import org.junit.Test;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;


/**
 * Tests for StackModule
 */
public class StackModuleTest {

  @Test
  public void stackServiceReposAreRead() throws Exception {
    StackModule sm = createStackModule("FooBar",
        "2.4",
        Optional.of(Lists.newArrayList(repoInfo("foo", "1.0.1", "http://foo.org"))),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org")));
    Set<String> repoIds = getIds(sm.getModuleInfo().getRepositories());
    assertEquals(ImmutableSet.of("foo:1.0.1", "bar:2.0.1"), repoIds);
  }

  /**
   * If more add-on services define the same repo, the duplicate repo definitions should be disregarded.
   * @throws Exception
   */
  @Test
  public void duplicateStackServiceReposAreDiscarded() throws Exception {
    StackModule sm = createStackModule("FooBar",
        "2.4",
        // stack repos
        Optional.of(Lists.newArrayList(repoInfo("StackRepoA", "1.1.1", "http://repos.org/stackrepoA"),
            repoInfo("StackRepoB", "2.2.2", "http://repos.org/stackrepoB"))),

        // stack service repos
        // These two should be preserved. even though duplicates, the contents are the same
        Lists.newArrayList(repoInfo("serviceRepoA", "1.0.0", "http://bar.org/1_0_0")),
        Lists.newArrayList(repoInfo("serviceRepoA", "1.0.0", "http://bar.org/1_0_0")),
        // These should be dropped as the names are the same but contents are different
        Lists.newArrayList(repoInfo("serviceRepoB", "1.2.1", "http://bar.org/1_1_1")),
        Lists.newArrayList(repoInfo("serviceRepoB", "1.2.3", "http://bar.org/1_1_1")),
        // The first one should be dropped (overrides a stack repo), the rest only generates warnings (duplicate urls)
        Lists.newArrayList(repoInfo("StackRepoA", "2.0.0", "http://repos.org/stackrepoA_200"),
            repoInfo("ShouldBeJustAWarning1", "3.1.1", "http://repos.org/stackrepoA"),
            repoInfo("ShouldBeJustAWarning2", "1.0.0", "http://bar.org/1_0_0")));
    List<RepositoryInfo> repos = sm.getModuleInfo().getRepositories();

    Set<String> repoIds = getIds(repos);
    assertEquals("Unexpected number of repos. Each repo should be added only once", repoIds.size(), repos.size());
    assertEquals("Unexpected repositories",
        ImmutableSet.of("StackRepoA:1.1.1",
          "StackRepoB:2.2.2",
          "serviceRepoA:1.0.0",
          "ShouldBeJustAWarning1:3.1.1",
          "ShouldBeJustAWarning2:1.0.0"), repoIds);
  }

  @Test
  public void serviceReposAreProcessedEvenIfNoStackRepo() throws Exception {
    StackModule sm = createStackModule("FooBar",
        "2.4",
        Optional.absent(),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org")));
    Set<String> repoIds = getIds(sm.getModuleInfo().getRepositories());
    assertEquals(ImmutableSet.of("bar:2.0.1"), repoIds);
  }

  /**
   * If two add-on services define the same repo, the repo should be disregarded.
   * This applies per os, so the same repo can be defined for multiple os'es (e.g redhat5 and redhat6)
   * @throws Exception
   */
  @Test
  public void duplicateStackServiceReposAreCheckedPerOs() throws Exception {
    StackModule sm = createStackModule("FooBar",
        "2.4",
        Optional.absent(),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos6")),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos7")));
    Multiset<String> repoIds = getIdsMultiple(sm.getModuleInfo().getRepositories());
    assertEquals("Repo should be occur exactly twice, once for each os type.",
        ImmutableMultiset.of("bar:2.0.1", "bar:2.0.1"), repoIds);
  }

  @Test
  public void removedServicesInitialValue () throws Exception {
    StackModule sm = createStackModule("FooBar",
        "2.4",
        Optional.absent(),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos6")),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos7")));
    List<String> removedServices = sm.getModuleInfo().getRemovedServices();
    assertEquals(removedServices.size(), 0);
  }

  @Test
  public void servicesWithNoConfigsInitialValue() throws Exception {
    StackModule sm = createStackModule("FooBar",
        "2.4",
        Optional.absent(),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos6")),
        Lists.newArrayList(repoInfo("bar", "2.0.1", "http://bar.org", "centos7")));
    List<String> servicesWithNoConfigs = sm.getModuleInfo().getServicesWithNoConfigs();
    assertEquals(servicesWithNoConfigs.size(), 0);
  }

  @SafeVarargs
  private static StackModule createStackModule(String stackName, String stackVersion, Optional<? extends List<RepositoryInfo>> stackRepos,
                                        List<RepositoryInfo>... serviceRepoLists) throws AmbariException {
    StackDirectory sd = mock(StackDirectory.class);
    List<ServiceDirectory> serviceDirectories = Lists.newArrayList();
    for (List<RepositoryInfo> serviceRepoList: serviceRepoLists) {
      StackServiceDirectory svd = mock(StackServiceDirectory.class);
      RepositoryXml serviceRepoXml = mock(RepositoryXml.class);
      when(svd.getRepoFile()).thenReturn(serviceRepoXml);
      when(serviceRepoXml.getRepositories()).thenReturn(serviceRepoList);
      ServiceMetainfoXml serviceMetainfoXml = mock(ServiceMetainfoXml.class);
      when(serviceMetainfoXml.isValid()).thenReturn(true);
      ServiceInfo serviceInfo = mock(ServiceInfo.class);
      when(serviceInfo.isValid()).thenReturn(true);
      when(serviceInfo.getName()).thenReturn(UUID.randomUUID().toString()); // unique service names
      when(serviceMetainfoXml.getServices()).thenReturn(Lists.newArrayList(serviceInfo));
      when(svd.getMetaInfoFile()).thenReturn(serviceMetainfoXml);
      serviceDirectories.add(svd);
    }
    if (stackRepos.isPresent()) {
      RepositoryXml stackRepoXml = mock(RepositoryXml.class);
      when(sd.getRepoFile()).thenReturn(stackRepoXml);
      when(stackRepoXml.getRepositories()).thenReturn(stackRepos.get());
    }
    when(sd.getServiceDirectories()).thenReturn(serviceDirectories);
    when(sd.getStackDirName()).thenReturn(stackName);
    when(sd.getDirectory()).thenReturn(new File(stackVersion));
    StackContext ctx = mock(StackContext.class);
    StackModule sm = new StackModule(sd, ctx);
    sm.resolve(null,
        ImmutableMap.of(String.format("%s:%s", stackName, stackVersion), sm),
        ImmutableMap.of(), ImmutableMap.of());
    return sm;
  }

  private RepositoryInfo repoInfo(String repoName, String repoVersion, String url) {
    return repoInfo(repoName, repoVersion, url, "centos6");
  }

  private List<RepositoryInfo> repoInfosForAllOs(String repoName, String repoVersion, String url) {
    List<RepositoryInfo> repos = new ArrayList<>(3);
    for (String os: new String[]{ "centos5", "centos6", "centos7"}) {
      repos.add(repoInfo(repoName, repoVersion, url, os));
    }
    return repos;
  }


  private RepositoryInfo repoInfo(String repoName, String repoVersion, String url, String osType) {
    RepositoryInfo info = new RepositoryInfo();
    info.setRepoId(String.format("%s:%s", repoName, repoVersion));
    info.setRepoName(repoName);
    info.setBaseUrl(url);
    info.setOsType(osType);
    return info;
  }

  private Set<String> getIds(List<RepositoryInfo> repoInfos) {
    return ImmutableSet.copyOf(Lists.transform(repoInfos, RepositoryInfo.GET_REPO_ID_FUNCTION));
  }

  private Multiset<String> getIdsMultiple(List<RepositoryInfo> repoInfos) {
    return ImmutableMultiset.copyOf(Lists.transform(repoInfos, RepositoryInfo.GET_REPO_ID_FUNCTION));
  }


}
