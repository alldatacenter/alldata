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
package org.apache.ambari.server.state.stack.upgrade;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.ambari.server.orm.dao.RepositoryVersionDAO;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.serveraction.upgrades.AutoSkipFailedSummaryAction;
import org.apache.ambari.server.stack.HostsType;
import org.apache.ambari.server.stack.upgrade.Direction;
import org.apache.ambari.server.stack.upgrade.Grouping;
import org.apache.ambari.server.stack.upgrade.ServerActionTask;
import org.apache.ambari.server.stack.upgrade.UpgradePack.ProcessingComponent;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapper;
import org.apache.ambari.server.stack.upgrade.orchestrate.StageWrapperBuilder;
import org.apache.ambari.server.stack.upgrade.orchestrate.UpgradeContext;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.spi.upgrade.UpgradeType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Assert;
import org.junit.Test;

/**
 * Tests the {@link StageWrapperBuilder}.
 */
public class StageWrapperBuilderTest extends EasyMockSupport {

  private static final StackId HDP_21 = new StackId("HDP-2.1.1");

  /**
   * Tests that the various build methods of a builder are invoked in the
   * correct order.
   *
   * @throws Exception
   */
  @Test
  public void testBuildOrder() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);

    RepositoryVersionEntity repoVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    EasyMock.expect(repoVersionEntity.getStackId()).andReturn(HDP_21).anyTimes();

    RepositoryVersionDAO repoVersionDAO = createNiceMock(RepositoryVersionDAO.class);
    EasyMock.expect(repoVersionDAO.findByStackNameAndVersion(EasyMock.anyString(),
        EasyMock.anyString())).andReturn(repoVersionEntity).anyTimes();

    UpgradeContext upgradeContext = EasyMock.createNiceMock(UpgradeContext.class);
    EasyMock.expect(upgradeContext.getCluster()).andReturn(cluster).anyTimes();
    EasyMock.expect(upgradeContext.getType()).andReturn(UpgradeType.ROLLING).anyTimes();
    EasyMock.expect(upgradeContext.getDirection()).andReturn(Direction.UPGRADE).anyTimes();
    EasyMock.expect(upgradeContext.getRepositoryVersion()).andReturn(repoVersionEntity).anyTimes();
    EasyMock.expect(upgradeContext.isComponentFailureAutoSkipped()).andReturn(false).anyTimes();
    EasyMock.expect(upgradeContext.isServiceCheckFailureAutoSkipped()).andReturn(false).anyTimes();

    replayAll();

    MockStageWrapperBuilder builder = new MockStageWrapperBuilder(null);
    List<StageWrapper> stageWrappers = builder.build(upgradeContext);
    List<Integer> invocationOrder = builder.getInvocationOrder();

    Assert.assertEquals(Integer.valueOf(0), invocationOrder.get(0));
    Assert.assertEquals(Integer.valueOf(1), invocationOrder.get(1));
    Assert.assertEquals(Integer.valueOf(2), invocationOrder.get(2));

    // nothing happened, so this should be empty
    Assert.assertTrue(stageWrappers.isEmpty());

    verifyAll();
  }

  /**
   * Tests that a new task was inserted into the upgrade which will check for
   * skipped failures and display a summary.
   *
   * @throws Exception
   */
  @Test
  public void testAutoSkipCheckInserted() throws Exception {
    Cluster cluster = createNiceMock(Cluster.class);

    RepositoryVersionEntity repoVersionEntity = createNiceMock(RepositoryVersionEntity.class);
    EasyMock.expect(repoVersionEntity.getStackId()).andReturn(HDP_21).anyTimes();

    RepositoryVersionDAO repoVersionDAO = createNiceMock(RepositoryVersionDAO.class);
    EasyMock.expect(repoVersionDAO.findByStackNameAndVersion(EasyMock.anyString(),
        EasyMock.anyString())).andReturn(repoVersionEntity).anyTimes();

    UpgradeContext upgradeContext = createNiceMock(UpgradeContext.class);
    EasyMock.expect(upgradeContext.getCluster()).andReturn(cluster).anyTimes();
    EasyMock.expect(upgradeContext.getType()).andReturn(UpgradeType.ROLLING).anyTimes();
    EasyMock.expect(upgradeContext.getDirection()).andReturn(Direction.UPGRADE).anyTimes();
    EasyMock.expect(upgradeContext.getRepositoryVersion()).andReturn(repoVersionEntity).anyTimes();
    EasyMock.expect(upgradeContext.isComponentFailureAutoSkipped()).andReturn(true).anyTimes();
    EasyMock.expect(upgradeContext.isServiceCheckFailureAutoSkipped()).andReturn(true).anyTimes();

    replayAll();

    Grouping grouping = new Grouping();
    grouping.skippable = true;

    MockStageWrapperBuilder builder = new MockStageWrapperBuilder(grouping);

    List<StageWrapper> mockStageWrappers = new ArrayList<>();
    StageWrapper mockStageWrapper = EasyMock.createNiceMock(StageWrapper.class);
    mockStageWrappers.add(mockStageWrapper);

    builder.setMockStageWrappers(mockStageWrappers);

    List<StageWrapper> stageWrappers = builder.build(upgradeContext);
    Assert.assertEquals(2, stageWrappers.size());

    StageWrapper skipSummaryWrapper = stageWrappers.get(1);
    Assert.assertEquals(StageWrapper.Type.SERVER_SIDE_ACTION, skipSummaryWrapper.getType());

    ServerActionTask task = (ServerActionTask)(skipSummaryWrapper.getTasks().get(0).getTasks().get(0));
    Assert.assertEquals(AutoSkipFailedSummaryAction.class.getName(), task.implClass);
    Assert.assertEquals(1, task.messages.size());
    Assert.assertTrue(task.messages.get(0).contains("There are failures that were automatically skipped"));

    verifyAll();
  }

  /**
   * A mock {@link StageWrapperBuilder}.
   */
  private final class MockStageWrapperBuilder extends StageWrapperBuilder {

    private List<Integer> m_invocationOrder = new ArrayList<>();
    private List<StageWrapper> m_stageWrappers = Collections.emptyList();

    /**
     * Constructor.
     *
     * @param grouping
     */
    protected MockStageWrapperBuilder(Grouping grouping) {
      super(grouping);
    }

    private void setMockStageWrappers(List<StageWrapper> stageWrappers) {
      m_stageWrappers = stageWrappers;
    }

    /**
     * Gets the invocation order.
     *
     * @return
     */
    private List<Integer> getInvocationOrder() {
      return m_invocationOrder;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void add(UpgradeContext upgradeContext, HostsType hostsType, String service,
        boolean clientOnly, ProcessingComponent pc, Map<String, String> params) {
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<StageWrapper> build(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {
      m_invocationOrder.add(1);
      return m_stageWrappers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<StageWrapper> beforeBuild(UpgradeContext upgradeContext) {
      List<StageWrapper> stageWrappers = super.beforeBuild(upgradeContext);
      m_invocationOrder.add(0);
      return stageWrappers;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected List<StageWrapper> afterBuild(UpgradeContext upgradeContext,
        List<StageWrapper> stageWrappers) {
      stageWrappers = super.afterBuild(upgradeContext, stageWrappers);
      m_invocationOrder.add(2);
      return stageWrappers;
    }
  }
}
