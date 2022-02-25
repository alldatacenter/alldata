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

package org.apache.ambari.server.serveraction.kerberos;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.persistence.EntityManager;

import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.controller.AmbariManagementController;
import org.apache.ambari.server.mpack.MpackManagerFactory;
import org.apache.ambari.server.security.SecurityHelper;
import org.apache.ambari.server.security.SecurityHelperImpl;
import org.apache.ambari.server.stack.StackManagerFactory;
import org.apache.ambari.server.state.Cluster;
import org.apache.ambari.server.state.Clusters;
import org.apache.ambari.server.state.ConfigHelper;
import org.apache.ambari.server.state.StackId;
import org.apache.ambari.server.state.cluster.ClustersImpl;
import org.apache.ambari.server.state.stack.OsFamily;
import org.apache.ambari.server.testutils.PartialNiceMockBinder;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;

public class UpdateKerberosConfigsServerActionTest extends EasyMockSupport{

  @Rule
  public TemporaryFolder testFolder = new TemporaryFolder();
  String dataDir;
  private Injector injector;
  private UpdateKerberosConfigsServerAction action;


  @Before
  public void setup() throws Exception {
    final Clusters clusters = createNiceMock(Clusters.class);
    final Cluster cluster = createNiceMock(Cluster.class);


    expect(clusters.getCluster(anyObject(String.class))).andReturn(cluster).once();

    injector = Guice.createInjector(new AbstractModule() {

      @Override
      protected void configure() {
        PartialNiceMockBinder.newBuilder(UpdateKerberosConfigsServerActionTest.this)
            .addClustersBinding().addLdapBindings().build().configure(binder());

        bind(ConfigHelper.class).toInstance(createNiceMock(ConfigHelper.class));
        bind(OsFamily.class).toInstance(createNiceMock(OsFamily.class));
        bind(Clusters.class).to(ClustersImpl.class);
        bind(EntityManager.class).toInstance(createNiceMock(EntityManager.class));
        bind(StackManagerFactory.class).toInstance(EasyMock.createNiceMock(StackManagerFactory.class));
        bind(MpackManagerFactory.class).toInstance(createNiceMock(MpackManagerFactory.class));
        bind(SecurityHelper.class).toInstance(SecurityHelperImpl.getInstance());
      }
    });

    final AmbariManagementController controller = injector.getInstance(AmbariManagementController.class);
    expect(controller.getClusters()).andReturn(clusters).once();

    dataDir = testFolder.getRoot().getAbsolutePath();

    setupConfigDat();

    action = injector.getInstance(UpdateKerberosConfigsServerAction.class);
  }

  private void setupConfigDat() throws Exception {
    File configFile = new File(dataDir, KerberosConfigDataFileWriter.DATA_FILE_NAME);
    KerberosConfigDataFileWriterFactory factory = injector.getInstance(KerberosConfigDataFileWriterFactory.class);
    KerberosConfigDataFileWriter writer = factory.createKerberosConfigDataFileWriter(configFile);
    writer.addRecord("hdfs-site", "hadoop.security.authentication", "kerberos", KerberosConfigDataFileWriter.OPERATION_TYPE_SET);
    writer.addRecord("hdfs-site", "remove.me", null, KerberosConfigDataFileWriter.OPERATION_TYPE_REMOVE);
    writer.close();
  }

  @Test
  public void testUpdateConfig() throws Exception {
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, dataDir);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);

    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);
    configHelper.updateConfigType(anyObject(Cluster.class), anyObject(StackId.class), anyObject(AmbariManagementController.class),
        anyObject(String.class), EasyMock.anyObject(), EasyMock.anyObject(), anyObject(String.class), anyObject(String.class));
    expectLastCall().atLeastOnce();

    replayAll();

    action.setExecutionCommand(executionCommand);
    action.execute(null);

    verifyAll();
  }

  @Test
  public void testUpdateConfigMissingDataDirectory() throws Exception {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<>();
    executionCommand.setCommandParams(commandParams);

    replayAll();

    action.setExecutionCommand(executionCommand);
    action.execute(null);

    verifyAll();
  }

  @Test
  public void testUpdateConfigEmptyDataDirectory() throws Exception {
    ExecutionCommand executionCommand = new ExecutionCommand();
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, testFolder.newFolder().getAbsolutePath());
    executionCommand.setCommandParams(commandParams);

    replayAll();

    action.setExecutionCommand(executionCommand);
    action.execute(null);

    verifyAll();
  }

  @Test
  public void testUpdateConfigForceSecurityEnabled() throws Exception {
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put(KerberosServerAction.DATA_DIRECTORY, dataDir);

    ExecutionCommand executionCommand = new ExecutionCommand();
    executionCommand.setCommandParams(commandParams);

    ConfigHelper configHelper = injector.getInstance(ConfigHelper.class);

    Capture<String> configTypes = Capture.newInstance(CaptureType.ALL);
    Capture<Map<String, String>> configUpdates = Capture.newInstance(CaptureType.ALL);
    configHelper.updateConfigType(anyObject(Cluster.class), anyObject(StackId.class), anyObject(AmbariManagementController.class),
        capture(configTypes), capture(configUpdates), anyObject(Collection.class), anyObject(String.class), anyObject(String.class));
    expectLastCall().atLeastOnce();

    replayAll();

    action.setExecutionCommand(executionCommand);
    action.execute(null);

    assertTrue(configTypes.getValues().contains("cluster-env"));
    boolean containsSecurityEnabled = false;
    for(Map<String, String> properties: configUpdates.getValues()) {
      if(properties.containsKey("security_enabled")) {
        containsSecurityEnabled = true;
        break;
      }
    }
    assertTrue(containsSecurityEnabled);
    verifyAll();
  }


}
