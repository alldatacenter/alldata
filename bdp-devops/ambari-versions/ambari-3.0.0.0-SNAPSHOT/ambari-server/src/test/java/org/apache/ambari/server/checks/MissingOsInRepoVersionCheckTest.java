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

package org.apache.ambari.server.checks;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.ambari.server.state.MaintenanceState.OFF;
import static org.easymock.EasyMock.anyInt;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.api.services.AmbariMetaInfo;
import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepoOsEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.Host;
import org.apache.ambari.server.state.RepositoryInfo;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.StackInfo;
import org.apache.ambari.spi.ClusterInformation;
import org.apache.ambari.spi.RepositoryType;
import org.apache.ambari.spi.RepositoryVersion;
import org.apache.ambari.spi.upgrade.UpgradeCheckRequest;
import org.apache.ambari.spi.upgrade.UpgradeCheckResult;
import org.apache.ambari.spi.upgrade.UpgradeCheckStatus;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.easymock.EasyMockRunner;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(EasyMockRunner.class)
public class MissingOsInRepoVersionCheckTest extends EasyMockSupport {
  public static final String CLUSTER_NAME = "cluster";
  public static final StackId SOURCE_STACK = new StackId("HDP-2.6");
  public static final String OS_FAMILY_IN_CLUSTER = "centos7";
  private MissingOsInRepoVersionCheck prerequisite;
  @Mock
  private Clusters clusters;
  @Mock
  private Cluster cluster;
  @Mock
  private Host host;
  @Mock
  private AmbariMetaInfo ambariMetaInfo;

  @Mock
  private RepositoryVersionDAO repositoryVersionDAO;

  private MockCheckHelper m_checkHelper = new MockCheckHelper();

  @Before
  public void setUp() throws Exception {
    prerequisite = new MissingOsInRepoVersionCheck();
    prerequisite.clustersProvider = () -> clusters;
    prerequisite.ambariMetaInfo = () -> ambariMetaInfo;
    prerequisite.checkHelperProvider = () -> m_checkHelper;
    prerequisite.repositoryVersionDaoProvider = () -> repositoryVersionDAO;

    expect(clusters.getCluster(CLUSTER_NAME)).andReturn(cluster).anyTimes();
    expect(cluster.getHosts()).andReturn(singleton(host)).anyTimes();
    expect(cluster.getClusterId()).andReturn(1l).anyTimes();
    expect(host.getOsFamily()).andReturn(OS_FAMILY_IN_CLUSTER).anyTimes();
    expect(host.getMaintenanceState(anyInt())).andReturn(OFF).anyTimes();

    m_checkHelper.m_clusters = clusters;
  }

  @Test
  public void testSuccessWhenOsExistsBothInTargetAndSource() throws Exception {
    sourceStackRepoIs(OS_FAMILY_IN_CLUSTER);
    UpgradeCheckRequest request = request(targetRepo(OS_FAMILY_IN_CLUSTER));
    replayAll();
    UpgradeCheckResult check = performPrerequisite(request);
    verifyAll();
    assertEquals(UpgradeCheckStatus.PASS, check.getStatus());
  }

  @Test
  public void testFailsWhenOsDoesntExistInSource() throws Exception {
    sourceStackRepoIs("different-os");
    UpgradeCheckRequest request = request(targetRepo(OS_FAMILY_IN_CLUSTER));
    replayAll();
    UpgradeCheckResult check = performPrerequisite(request);
    assertEquals(UpgradeCheckStatus.FAIL, check.getStatus());
    verifyAll();
  }

  @Test
  public void testFailsWhenOsDoesntExistInTarget() throws Exception {
    sourceStackRepoIs(OS_FAMILY_IN_CLUSTER);
    UpgradeCheckRequest request = request(targetRepo("different-os"));
    replayAll();
    UpgradeCheckResult check = performPrerequisite(request);
    assertEquals(UpgradeCheckStatus.FAIL, check.getStatus());
    verifyAll();
  }

  private void sourceStackRepoIs(String osFamily) throws AmbariException {
    expect(ambariMetaInfo.getStack(SOURCE_STACK)).andReturn(stackInfo(repoInfo(osFamily))).anyTimes();
  }

  private StackInfo stackInfo(RepositoryInfo repositoryInfo) {
    StackInfo stackInfo = new StackInfo();
    stackInfo.getRepositories().add(repositoryInfo);
    return stackInfo;
  }

  private RepositoryInfo repoInfo(String osType) {
    RepositoryInfo repo = new RepositoryInfo();
    repo.setOsType(osType);
    return repo;
  }

  private UpgradeCheckRequest request(RepositoryVersionEntity targetRepo) {
    m_checkHelper.m_repositoryVersionDAO = repositoryVersionDAO;
    expect(repositoryVersionDAO.findByPK(1L)).andReturn(targetRepo).anyTimes();

    RepositoryVersion repositoryVersion = createNiceMock(RepositoryVersion.class);
    expect(repositoryVersion.getId()).andReturn(1L).anyTimes();
    expect(repositoryVersion.getRepositoryType()).andReturn(RepositoryType.STANDARD).anyTimes();
    expect(repositoryVersion.getStackId()).andReturn(SOURCE_STACK.getStackId()).anyTimes();

    ClusterInformation clusterInformation = new ClusterInformation(CLUSTER_NAME, false, null, null, null);
    UpgradeCheckRequest request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING,
        repositoryVersion, null, null);

    return request;
  }

  private RepositoryVersionEntity targetRepo(String osFamilyInCluster) {
    RepositoryVersionEntity targetRepo = new RepositoryVersionEntity();
    RepoOsEntity osEntity = new RepoOsEntity();
    osEntity.setFamily(osFamilyInCluster);
    targetRepo.addRepoOsEntities(singletonList(osEntity));
    return targetRepo;
  }

  private UpgradeCheckResult performPrerequisite(UpgradeCheckRequest request) throws AmbariException {
    return prerequisite.perform(request);
  }
}