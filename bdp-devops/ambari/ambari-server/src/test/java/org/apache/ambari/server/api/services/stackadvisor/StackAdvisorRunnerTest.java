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

package org.apache.ambari.server.api.services.stackadvisor;

import static org.easymock.EasyMock.expect;
import static org.junit.Assert.fail;
import static org.powermock.api.easymock.PowerMock.createNiceMock;
import static org.powermock.api.easymock.PowerMock.replay;
import static org.powermock.api.support.membermodification.MemberModifier.stub;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import org.apache.ambari.server.api.services.stackadvisor.commands.StackAdvisorCommandType;
import org.apache.ambari.server.configuration.Configuration;
import org.apache.ambari.server.state.ServiceInfo;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.powermock.api.easymock.PowerMock;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

/**
 * StackAdvisorRunner unit tests.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(StackAdvisorRunner.class)
public class StackAdvisorRunnerTest {

  private TemporaryFolder temp = new TemporaryFolder();

  @Before
  public void setUp() throws IOException {
    temp.create();
  }

  @After
  public void tearDown() throws IOException {
    temp.delete();
  }

  @Test(expected = StackAdvisorException.class)
  public void testRunScript_processStartThrowsException_returnFalse() throws Exception {
    StackAdvisorCommandType saCommandType = StackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    File actionDirectory = temp.newFolder("actionDir");
    ProcessBuilder processBuilder = createNiceMock(ProcessBuilder.class);
    StackAdvisorRunner saRunner = new StackAdvisorRunner();
    Configuration configMock = createNiceMock(Configuration.class);
    saRunner.setConfigs(configMock);
    stub(PowerMock.method(StackAdvisorRunner.class, "prepareShellCommand"))
        .toReturn(processBuilder);
    expect(processBuilder.environment()).andReturn(new HashMap<>()).times(3);
    expect(processBuilder.start()).andThrow(new IOException());
    replay(processBuilder, configMock);
    saRunner.runScript(ServiceInfo.ServiceAdvisorType.PYTHON, saCommandType, actionDirectory);
  }

  @Test(expected = StackAdvisorRequestException.class)
  public void testRunScript_processExitCode1_returnFalse() throws Exception {
    StackAdvisorCommandType saCommandType = StackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    File actionDirectory = temp.newFolder("actionDir");
    ProcessBuilder processBuilder = createNiceMock(ProcessBuilder.class);
    Process process = createNiceMock(Process.class);
    StackAdvisorRunner saRunner = new StackAdvisorRunner();
    Configuration configMock = createNiceMock(Configuration.class);
    saRunner.setConfigs(configMock);
    stub(PowerMock.method(StackAdvisorRunner.class, "prepareShellCommand"))
        .toReturn(processBuilder);
    expect(processBuilder.environment()).andReturn(new HashMap<>()).times(3);
    expect(processBuilder.start()).andReturn(process);
    expect(process.waitFor()).andReturn(1);
    replay(processBuilder, process, configMock);
    saRunner.runScript(ServiceInfo.ServiceAdvisorType.PYTHON, saCommandType, actionDirectory);
  }

  @Test(expected = StackAdvisorException.class)
  public void testRunScript_processExitCode2_returnFalse() throws Exception {
    StackAdvisorCommandType saCommandType = StackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    File actionDirectory = temp.newFolder("actionDir");
    ProcessBuilder processBuilder = createNiceMock(ProcessBuilder.class);
    Process process = createNiceMock(Process.class);
    StackAdvisorRunner saRunner = new StackAdvisorRunner();
    Configuration configMock = createNiceMock(Configuration.class);
    saRunner.setConfigs(configMock);

    stub(PowerMock.method(StackAdvisorRunner.class, "prepareShellCommand"))
        .toReturn(processBuilder);
    expect(processBuilder.environment()).andReturn(new HashMap<>()).times(3);
    expect(processBuilder.start()).andReturn(process);
    expect(process.waitFor()).andReturn(2);
    replay(processBuilder, process, configMock);
    saRunner.runScript(ServiceInfo.ServiceAdvisorType.PYTHON, saCommandType, actionDirectory);
  }

  @Test
  public void testRunScript_processExitCodeZero_returnTrue() throws Exception {
    StackAdvisorCommandType saCommandType = StackAdvisorCommandType.RECOMMEND_COMPONENT_LAYOUT;
    File actionDirectory = temp.newFolder("actionDir");
    ProcessBuilder processBuilder = createNiceMock(ProcessBuilder.class);
    Process process = createNiceMock(Process.class);
    StackAdvisorRunner saRunner = new StackAdvisorRunner();
    Configuration configMock = createNiceMock(Configuration.class);
    saRunner.setConfigs(configMock);

    stub(PowerMock.method(StackAdvisorRunner.class, "prepareShellCommand"))
        .toReturn(processBuilder);
    expect(processBuilder.environment()).andReturn(new HashMap<>()).times(3);
    expect(processBuilder.start()).andReturn(process);
    expect(process.waitFor()).andReturn(0);
    replay(processBuilder, process, configMock);
    try {
      saRunner.runScript(ServiceInfo.ServiceAdvisorType.PYTHON, saCommandType, actionDirectory);
    } catch (StackAdvisorException ex) {
      fail("Should not fail with StackAdvisorException");
    }
  }
}
