/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.ambari.server.serveraction.users;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.Role;
import org.apache.ambari.server.RoleCommand;
import org.apache.ambari.server.actionmanager.HostRoleCommand;
import org.apache.ambari.server.agent.CommandReport;
import org.apache.ambari.server.agent.ExecutionCommand;
import org.apache.ambari.server.hooks.users.UserHookParams;
import org.apache.ambari.server.state.SecurityType;
import org.apache.ambari.server.utils.ShellCommandUtil;
import org.codehaus.jackson.map.ObjectMapper;
import org.easymock.Capture;
import org.easymock.EasyMock;
import org.easymock.EasyMockRule;
import org.easymock.EasyMockSupport;
import org.easymock.Mock;
import org.easymock.TestSubject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * Test suite for the PostUserCreationHookServer action class.
 */
public class PostUserCreationHookServerActionTest extends EasyMockSupport {

  private static final Logger LOGGER = LoggerFactory.getLogger(PostUserCreationHookServerActionTest.class);

  @Rule
  public EasyMockRule mocks = new EasyMockRule(this);

  @Mock
  private ShellCommandUtilityWrapper shellCommandUtilityWrapper;

  @Mock
  private ExecutionCommand executionCommand;

  @Mock
  private HostRoleCommand hostRoleCommand;

  @Mock
  private ObjectMapper objectMapperMock;

  @Mock
  private CollectionPersisterServiceFactory collectionPersisterServiceFactoryMock;

  @Mock
  private CsvFilePersisterService collectionPersisterService;

  @TestSubject
  private PostUserCreationHookServerAction customScriptServerAction = new PostUserCreationHookServerAction();

  private ConcurrentMap<String, Object> requestSharedDataContext = Maps.newConcurrentMap();

  private Capture<String[]> commandCapture = null;

  private Map<String, List<String>> payload = new HashMap<>();

  private ObjectMapper om = new ObjectMapper();

  @Before
  public void before() throws IOException, InterruptedException {
    payload.clear();
    resetAll();

    EasyMock.expect(hostRoleCommand.getRequestId()).andReturn(-1l).times(2);
    EasyMock.expect(hostRoleCommand.getStageId()).andReturn(-1l).times(2);
  }


  @Test
  public void shouldCommandStringBeAssembledCorrectlyForSingleUser() throws Exception {
    // GIVEN

    payload = mockPayload(1);
    mockExecutionCommand(payload.size());
    String payloadJson = om.writeValueAsString(payload);

    // command params as passed to the serveraction implementation
    Map<String, String> commandParams = new HashMap<>();
    commandParams.put(UserHookParams.PAYLOAD.param(), payloadJson);
    commandParams.put(UserHookParams.SCRIPT.param(), "/hookfolder/hook.name");
    commandParams.put(UserHookParams.CMD_TIME_FRAME.param(), "1000");
    commandParams.put(UserHookParams.CMD_INPUT_FILE.param(), "/test/user_data.csv");
    commandParams.put(UserHookParams.CLUSTER_SECURITY_TYPE.param(), SecurityType.KERBEROS.name());
    commandParams.put(UserHookParams.CMD_HDFS_USER.param(), "test-hdfs-user");

    EasyMock.expect(executionCommand.getCommandParams()).andReturn(commandParams);
    EasyMock.expect(objectMapperMock.readValue(payloadJson, Map.class)).andReturn(payload);

    // captures the command arguments passed to the shell callable through the factory
    commandCapture = EasyMock.newCapture();

    // the callable mock returns a dummy result, no assertions made on the result
    EasyMock.expect(shellCommandUtilityWrapper.runCommand(EasyMock.capture(commandCapture))).andReturn(new ShellCommandUtil.Result(0, null, null)).times(payload.size());

    customScriptServerAction.setExecutionCommand(executionCommand);

    EasyMock.expect(collectionPersisterServiceFactoryMock.createCsvFilePersisterService(EasyMock.anyString())).andReturn(collectionPersisterService);
    EasyMock.expect(collectionPersisterService.persistMap(EasyMock.anyObject())).andReturn(Boolean.TRUE);

    replayAll();

    // WHEN
    CommandReport commandReport = customScriptServerAction.execute(requestSharedDataContext);

    // THEN
    String[] commandArray = commandCapture.getValue();
    Assert.assertNotNull("The command to be executed must not be null!", commandArray);

    Assert.assertEquals("The command argument array length is not as expected!", 6, commandArray.length);
    Assert.assertEquals("The command script is not as expected", "/hookfolder/hook.name", commandArray[0]);
  }


  @Test(expected = AmbariException.class)
  public void shouldServerActionFailWhenCommandParametersAreMissing() throws Exception {
    //GIVEN
    Map<String, String> commandParams = new HashMap<>();
    // the execution command lacks the required command parameters (commandparams is an empty list)
    EasyMock.expect(executionCommand.getCommandParams()).andReturn(commandParams).times(2);

    customScriptServerAction.setExecutionCommand(executionCommand);
    replayAll();

    // WHEN
    CommandReport commandReport = customScriptServerAction.execute(requestSharedDataContext);

    //THEN
    //exception is thrown
  }

  private void mockExecutionCommand(int callCnt) {
    EasyMock.expect(executionCommand.getClusterId()).andReturn("1").anyTimes();
    EasyMock.expect(executionCommand.getRoleCommand()).andReturn(RoleCommand.EXECUTE).times(callCnt);
    EasyMock.expect(executionCommand.getClusterName()).andReturn("unit-test-cluster").times(callCnt);
    EasyMock.expect(executionCommand.getRole()).andReturn(Role.AMBARI_SERVER_ACTION.toString()).times(callCnt);
    EasyMock.expect(executionCommand.getServiceName()).andReturn("custom-hook-script").times(callCnt);
    EasyMock.expect(executionCommand.getTaskId()).andReturn(-1l).times(callCnt);
  }

  private Map<String, List<String>> mockPayload(int size) {
    Map<String, List<String>> ret = new HashMap<>();
    for (int i = 0; i < size; i++) {
      ret.put("user-" + i, Arrays.asList("hdfs" + i, "yarn" + i));
    }
    return ret;
  }


}
