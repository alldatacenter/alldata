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

package org.apache.ambari.server.stack;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.apache.ambari.server.AmbariException;
import org.junit.Test;

/**
 * Tests for StackServiceDirectory
 */
public class StackServiceDirectoryTest {

  private MockStackServiceDirectory createStackServiceDirectory(String servicePath) throws AmbariException {
    MockStackServiceDirectory ssd = new MockStackServiceDirectory(servicePath);
    return ssd;
  }

  @Test
  public void testValidServiceAdvisorClassName() throws Exception {
    String pathWithInvalidChars = "/Fake-Stack.Name/1.0/services/FAKESERVICE/";
    String serviceNameValidChars = "FakeService";

    String pathWithValidChars = "/FakeStackName/1.0/services/FAKESERVICE/";
    String serviceNameInvalidChars = "Fake-Serv.ice";

    String desiredServiceAdvisorName = "FakeStackName10FakeServiceServiceAdvisor";

    MockStackServiceDirectory ssd1 = createStackServiceDirectory(pathWithInvalidChars);
    assertEquals(desiredServiceAdvisorName, ssd1.getAdvisorName(serviceNameValidChars));

    MockStackServiceDirectory ssd2 = createStackServiceDirectory(pathWithValidChars);
    assertEquals(desiredServiceAdvisorName, ssd2.getAdvisorName(serviceNameInvalidChars));

    MockStackServiceDirectory ssd3 = createStackServiceDirectory(pathWithInvalidChars);
    assertEquals(desiredServiceAdvisorName, ssd3.getAdvisorName(serviceNameInvalidChars));

    MockStackServiceDirectory ssd4 = createStackServiceDirectory(pathWithValidChars);
    assertEquals(desiredServiceAdvisorName, ssd4.getAdvisorName(serviceNameValidChars));
  }

  private class MockStackServiceDirectory extends StackServiceDirectory {
    File advisor = null;

    MockStackServiceDirectory (String servicePath) throws AmbariException {
      super(servicePath);
      advisor = new File(servicePath, StackDirectory.SERVICE_ADVISOR_FILE_NAME);
    }

    protected void parsePath() {}

    public File getAdvisorFile() {
      return advisor;
    }
  }
}
