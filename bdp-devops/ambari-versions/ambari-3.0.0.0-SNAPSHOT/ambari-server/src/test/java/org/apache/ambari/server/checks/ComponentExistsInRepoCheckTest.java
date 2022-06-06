/**
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
package org.apache.ambari.server.checks;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expect;
import static org.junit.Assert.assertEquals;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.serveraction.upgrades.DeleteUnsupportedServicesAndComponents;
import org.apache.ambari.server.stack.upgrade.ClusterGrouping;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.ExecuteStage;
import org.apache.ambari.server.stack.upgrade.ServerActionTask;
import org.apache.ambari.server.stack.upgrade.UpgradePack;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeHelper;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ServiceComponentSupport;
import org.apache.ambari.server.state.StackId;
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
public class ComponentExistsInRepoCheckTest extends EasyMockSupport {
  public static final String STACK_NAME = "HDP";
  public static final String STACK_VERSION = "2.2.0";
  private ComponentsExistInRepoCheck check = new ComponentsExistInRepoCheck();
  @Mock
  private Clusters clusters;
  @Mock
  private Cluster cluster;
  @Mock
  private ServiceComponentSupport serviceComponentSupport;
  @Mock
  private UpgradeHelper upgradeHelper;
  private UpgradeCheckRequest request;

  private StackId sourceStackId = new StackId(STACK_NAME, "1.0");

  @Before
  public void before() throws Exception {
    check.clustersProvider = () -> clusters;
    check.serviceComponentSupport = serviceComponentSupport;
    check.upgradeHelper = upgradeHelper;
    expect(clusters.getCluster((String) anyObject())).andReturn(cluster).anyTimes();

    expect(cluster.getCurrentStackVersion()).andReturn(sourceStackId).anyTimes();

    ClusterInformation clusterInformation = new ClusterInformation("cluster", false, null, null, null);
    request = new UpgradeCheckRequest(clusterInformation, UpgradeType.ROLLING, repoVersion(), null, null);
  }

  @Test
  public void testPassesWhenNoUnsupportedInTargetStack() throws Exception {
    expect(serviceComponentSupport.allUnsupported(cluster, STACK_NAME, STACK_VERSION)).andReturn(emptyList()).anyTimes();
    replayAll();
    UpgradeCheckResult result = check.perform(request);
    assertEquals(UpgradeCheckStatus.PASS, result.getStatus());
  }

  @Test
  public void testFailsWhenUnsupportedFoundInTargetStack() throws Exception {
    expect(serviceComponentSupport.allUnsupported(cluster, STACK_NAME, STACK_VERSION)).andReturn(singletonList("ANY_SERVICE")).anyTimes();
    suggestedUpgradePackIs(new UpgradePack());
    replayAll();
    UpgradeCheckResult result = check.perform(request);
    assertEquals(UpgradeCheckStatus.FAIL, result.getStatus());
  }

  @Test
  public void testWarnsWhenUnsupportedFoundInTargetStackAndUpgradePackHasAutoDeleteTask() throws Exception {
    expect(serviceComponentSupport.allUnsupported(cluster, STACK_NAME, STACK_VERSION)).andReturn(singletonList("ANY_SERVICE")).anyTimes();
    suggestedUpgradePackIs(upgradePackWithDeleteUnsupportedTask());
    replayAll();
    UpgradeCheckResult result = check.perform(request);
    assertEquals(UpgradeCheckStatus.WARNING, result.getStatus());
  }

  private RepositoryVersion repoVersion() {
    RepositoryVersion repositoryVersion = new RepositoryVersion(1, STACK_NAME, STACK_VERSION,
        new StackId(STACK_NAME, STACK_VERSION).getStackId(), STACK_VERSION,
        RepositoryType.STANDARD);
    return repositoryVersion;
  }

  private void suggestedUpgradePackIs(UpgradePack upgradePack) throws AmbariException {
    expect(upgradeHelper.suggestUpgradePack(
      "cluster",
      sourceStackId,
      new StackId(request.getTargetRepositoryVersion().getStackId()),
      Direction.UPGRADE,
      request.getUpgradeType(),
      null)).andReturn(upgradePack).anyTimes();
  }

  private UpgradePack upgradePackWithDeleteUnsupportedTask() {
    UpgradePack upgradePack = new UpgradePack();
    ClusterGrouping group = new ClusterGrouping();
    ExecuteStage stage = new ExecuteStage();
    ServerActionTask task = new ServerActionTask();
    task.setImplClass(DeleteUnsupportedServicesAndComponents.class.getName());
    stage.task = task;
    group.executionStages = singletonList(stage);
    upgradePack.getAllGroups().add(group);
    return upgradePack;
  }
}