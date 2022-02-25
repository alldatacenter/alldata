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

package org.apache.ambari.server;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.sql.SQLException;

import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.orm.dao.HostVersionDAO;
import org.apache.ambari.server.orm.dao.ServiceComponentDesiredStateDAO;
import org.apache.ambari.server.orm.entities.HostVersionEntity;
import org.apache.ambari.server.orm.entities.RepositoryVersionEntity;
import org.apache.ambari.server.orm.entities.ServiceComponentDesiredStateEntity;
import org.apache.ambari.server.state.RepositoryVersionState;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.util.Modules;

public class StateRecoveryManagerTest {

  private Injector injector;
  private HostVersionDAO hostVersionDAOMock;
  private ServiceComponentDesiredStateDAO serviceComponentDesiredStateDAOMock;

  @Before
  public void setup() throws Exception {
    // Create instances of mocks
    hostVersionDAOMock = createNiceMock(HostVersionDAO.class);
    serviceComponentDesiredStateDAOMock = createNiceMock(ServiceComponentDesiredStateDAO.class);
    // Initialize injector
    InMemoryDefaultTestModule module = new InMemoryDefaultTestModule();
    injector = Guice.createInjector(Modules.override(module).with(new MockModule()));
    injector.getInstance(GuiceJpaInitializer.class);
  }

  @After
  public void teardown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }

  @Test
  public void testCheckHostAndClusterVersions() throws Exception {
    StateRecoveryManager stateRecoveryManager = injector.getInstance(StateRecoveryManager.class);

    // Adding all possible host version states

    final Capture<RepositoryVersionState> installFailedHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> installingHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> installedHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> outOfSyncHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradeFailedHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradingHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradedHostVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> currentHostVersionCapture = EasyMock.newCapture();

    expect(hostVersionDAOMock.findAll()).andReturn(Lists.newArrayList(
      getHostVersionMock("install_failed_version", RepositoryVersionState.INSTALL_FAILED, installFailedHostVersionCapture),
      getHostVersionMock("installing_version", RepositoryVersionState.INSTALLING, installingHostVersionCapture),
      getHostVersionMock("installed_version", RepositoryVersionState.INSTALLED, installedHostVersionCapture),
        getHostVersionMock("out_of_sync_version", RepositoryVersionState.OUT_OF_SYNC,
            outOfSyncHostVersionCapture)));

    // Adding all possible cluster version states

    final Capture<RepositoryVersionState> installFailedClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> installingClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> installedClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> outOfSyncClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradeFailedClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradingClusterVersionCapture = EasyMock.newCapture();
    final Capture<RepositoryVersionState> upgradedClusterVersionCapture = EasyMock.newCapture();

    expect(serviceComponentDesiredStateDAOMock.findAll()).andReturn(Lists.newArrayList(
      getDesiredStateEntityMock("install_failed_version", RepositoryVersionState.INSTALL_FAILED, installFailedClusterVersionCapture),
      getDesiredStateEntityMock("installing_version", RepositoryVersionState.INSTALLING, installingClusterVersionCapture),
      getDesiredStateEntityMock("installed_version", RepositoryVersionState.INSTALLED, installedClusterVersionCapture),
        getDesiredStateEntityMock("out_of_sync_version", RepositoryVersionState.OUT_OF_SYNC,
            outOfSyncClusterVersionCapture)));

    replay(hostVersionDAOMock, serviceComponentDesiredStateDAOMock);

    stateRecoveryManager.checkHostAndClusterVersions();

    // Checking that only invalid host version states have been changed
    assertFalse(installFailedHostVersionCapture.hasCaptured());
    assertEquals(installingHostVersionCapture.getValue(), RepositoryVersionState.INSTALL_FAILED);
    assertFalse(installedHostVersionCapture.hasCaptured());
    assertFalse(outOfSyncHostVersionCapture.hasCaptured());
    assertFalse(upgradeFailedHostVersionCapture.hasCaptured());
    assertFalse(upgradingHostVersionCapture.hasCaptured());
    assertFalse(upgradedHostVersionCapture.hasCaptured());
    assertFalse(currentHostVersionCapture.hasCaptured());

    // Checking that only invalid cluster version states have been changed
    assertFalse(installFailedClusterVersionCapture.hasCaptured());
    assertEquals(installingClusterVersionCapture.getValue(), RepositoryVersionState.INSTALL_FAILED);
    assertFalse(installedClusterVersionCapture.hasCaptured());
    assertFalse(outOfSyncClusterVersionCapture.hasCaptured());
    assertFalse(upgradeFailedClusterVersionCapture.hasCaptured());
    assertFalse(upgradingClusterVersionCapture.hasCaptured());
    assertFalse(upgradedClusterVersionCapture.hasCaptured());
  }


  private HostVersionEntity getHostVersionMock(String name, RepositoryVersionState state,
                                               Capture<RepositoryVersionState> newStateCaptor) {
    HostVersionEntity hvMock = createNiceMock(HostVersionEntity.class);
    expect(hvMock.getState()).andReturn(state);

    hvMock.setState(capture(newStateCaptor));
    expectLastCall();

    RepositoryVersionEntity rvMock = createNiceMock(RepositoryVersionEntity.class);
    expect(rvMock.getDisplayName()).andReturn(name);

    expect(hvMock.getRepositoryVersion()).andReturn(rvMock);
    expect(hvMock.getHostName()).andReturn("somehost");

    replay(hvMock, rvMock);

    return hvMock;
  }

  private ServiceComponentDesiredStateEntity getDesiredStateEntityMock(String name, RepositoryVersionState state, Capture<RepositoryVersionState> newStateCapture) {

    ServiceComponentDesiredStateEntity mock = createNiceMock(ServiceComponentDesiredStateEntity.class);
    expect(mock.getRepositoryState()).andReturn(state);
    mock.setRepositoryState(capture(newStateCapture));
    expectLastCall();

    RepositoryVersionEntity repositoryVersionMock = createNiceMock(RepositoryVersionEntity.class);
    expect(repositoryVersionMock.getVersion()).andReturn(name);

    expect(mock.getDesiredRepositoryVersion()).andReturn(repositoryVersionMock);

    replay(mock, repositoryVersionMock);

    return mock;
  }



  public class MockModule extends AbstractModule {
    @Override
    protected void configure() {
      bind(HostVersionDAO.class).toInstance(hostVersionDAOMock);
      bind(ServiceComponentDesiredStateDAO.class).toInstance(serviceComponentDesiredStateDAOMock);
    }
  }

}
