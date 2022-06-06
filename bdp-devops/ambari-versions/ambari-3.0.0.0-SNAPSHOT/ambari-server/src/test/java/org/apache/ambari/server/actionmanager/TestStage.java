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
import java.util.Map;
import java.util.TreeMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.H2DatabaseCleaner;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.orm.GuiceJpaInitializer;
import org.apache.ambari.server.orm.InMemoryDefaultTestModule;
import org.apache.ambari.server.utils.StageUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;

public class TestStage {

  private static final String CLUSTER_HOST_INFO = "cluster_host_info";

  Injector injector;

  @Inject
  StageFactory stageFactory;

  @Inject
  StageUtils stageUtils;

  @Before
  public void setup() throws Exception {
    injector = Guice.createInjector(new InMemoryDefaultTestModule());
    injector.getInstance(GuiceJpaInitializer.class);
    injector.injectMembers(this);
  }

  @Test
  public void testTaskTimeout() {
    Stage s = StageUtils.getATestStage(1, 1, "h1",  "{\"host_param\":\"param_value\"}", "{\"stage_param\":\"param_value\"}");
    s.addHostRoleExecutionCommand("h1", Role.DATANODE, RoleCommand.INSTALL,
        null, "c1", "HDFS", false, false);
    s.addHostRoleExecutionCommand("h1", Role.HBASE_MASTER, RoleCommand.INSTALL,
        null, "c1", "HBASE", false, false);
    for (ExecutionCommandWrapper wrapper : s.getExecutionCommands("h1")) {
      Map<String, String> commandParams = new TreeMap<>();
      commandParams.put(ExecutionCommand.KeyNames.COMMAND_TIMEOUT, "600");
      wrapper.getExecutionCommand().setCommandParams(commandParams);
    }
    assertEquals(3*600000, s.getStageTimeout());
  }

  @Test
  public void testGetRequestContext() {
    Stage stage = stageFactory.createNew(1, "/logDir", "c1", 1L, "My Context",  "", "");
    assertEquals("My Context", stage.getRequestContext());
  }

  @After
  public void tearDown() throws AmbariException, SQLException {
    H2DatabaseCleaner.clearDatabaseAndStopPersistenceService(injector);
  }
}
