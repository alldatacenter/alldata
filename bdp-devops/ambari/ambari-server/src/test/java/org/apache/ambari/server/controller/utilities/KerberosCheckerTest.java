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

package org.apache.ambari.server.controller.utilities;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;

import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.easymock.EasyMockSupport;
import org.junit.Test;

public class KerberosCheckerTest extends EasyMockSupport {

  @Test
  public void testCheckPassed() throws Exception {
    Configuration config = createMock(Configuration.class);
    LoginContextHelper loginContextHelper = createMock(LoginContextHelper.class);
    LoginContext lc = createMock(LoginContext.class);

    expect(config.isKerberosJaasConfigurationCheckEnabled()).andReturn(true).once();

    expect(loginContextHelper.createLoginContext(KerberosChecker.HTTP_SPNEGO_STANDARD_ENTRY)).andReturn(lc).once();

    lc.login();
    expectLastCall().once();
    lc.logout();
    expectLastCall().once();

    replayAll();

    KerberosChecker.config = config;
    KerberosChecker.loginContextHelper = loginContextHelper;
    KerberosChecker.checkJaasConfiguration();

    verifyAll();
  }

  @Test(expected = AmbariException.class)
  public void testCheckFailed() throws Exception {
    Configuration config = createMock(Configuration.class);
    LoginContextHelper loginContextHelper = createMock(LoginContextHelper.class);

    expect(config.isKerberosJaasConfigurationCheckEnabled()).andReturn(true).once();

    expect(loginContextHelper.createLoginContext(KerberosChecker.HTTP_SPNEGO_STANDARD_ENTRY)).andThrow(new LoginException()).once();

    replayAll();

    KerberosChecker.config = config;
    KerberosChecker.loginContextHelper = loginContextHelper;
    KerberosChecker.checkJaasConfiguration();

    verifyAll();
  }
}
