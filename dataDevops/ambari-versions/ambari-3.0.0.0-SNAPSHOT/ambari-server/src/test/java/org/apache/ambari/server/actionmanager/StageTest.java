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

package org.apache.ambari.server.actionmanager;

import static org.junit.Assert.assertEquals;

import java.sql.SQLException;
import java.util.Collections;
import java.util.List;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.serveraction.ServerAction;
import org.apache.ambari.server.serveraction.upgrades.ConfigureAction;
import org.apache.ambari.server.state.svccomphost.ServiceComponentHostServerActionEvent;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Stage tests.
 */
public class StageTest {

  private static final String SERVER_HOST_NAME = StageUtils.getHostName();
  private static final String CLUSTER_HOST_INFO = "{all_hosts=["
      + SERVER_HOST_NAME + "], slave_hosts=["
      + SERVER_HOST_NAME + "]}";

  Injector injector;

  @Inject
  StageFactory stageFactory;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @Test
  public void testAddServerActionCommand_userName() throws Exception {
    final Stage stage = stageFactory.createNew(1, "/tmp", "cluster1", 978, "context",
        "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");

    stage.addServerActionCommand(ConfigureAction.class.getName(),
        "user1", Role.AMBARI_SERVER_ACTION,
        RoleCommand.EXECUTE,
        "cluster1",
        new ServiceComponentHostServerActionEvent(StageUtils.getHostName(), System.currentTimeMillis()),
        Collections.emptyMap(), null, null, 1200, false, false);

    List<ExecutionCommandWrapper> executionCommands = stage.getExecutionCommands(null);
    assertEquals(1, executionCommands.size());

    String actionUserName = executionCommands.get(0).getExecutionCommand().getRoleParams().get(ServerAction.ACTION_USER_NAME);

    assertEquals("user1", actionUserName);
  }

  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }
}
