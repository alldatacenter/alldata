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
package org.apache.drill.exec.rpc.user.security.testing;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.rpc.user.security.UserAuthenticationException;
import org.apache.drill.exec.rpc.user.security.UserAuthenticator;
import org.apache.drill.exec.rpc.user.security.UserAuthenticatorTemplate;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.IOException;

import static org.apache.drill.exec.impersonation.TestInboundImpersonation.PROXY_NAME;
import static org.apache.drill.exec.impersonation.TestInboundImpersonation.PROXY_PASSWORD;
import static org.apache.drill.exec.impersonation.TestInboundImpersonation.TARGET_NAME;
import static org.apache.drill.exec.impersonation.TestInboundImpersonation.TARGET_PASSWORD;
import static org.apache.drill.exec.impersonation.TestInboundImpersonation.OWNER;
import static org.apache.drill.exec.impersonation.TestInboundImpersonation.OWNER_PASSWORD;

/**
 * Implement {@link org.apache.drill.exec.rpc.user.security.UserAuthenticator} for testing:
 * + UserAuthenticator and authentication of users from Java client to Drillbit.
 * + {@link TestInboundImpersonation user delegation}.
 */
@UserAuthenticatorTemplate(type = UserAuthenticatorTestImpl.TYPE)
public class UserAuthenticatorTestImpl implements UserAuthenticator {
  public static final String TYPE = "drillTestAuthenticator";

  public static final String TEST_USER_1 = "testUser1";
  public static final String TEST_USER_2 = "testUser2";
  public static final String ADMIN_USER = "admin";
  public static final String PROCESS_USER = ImpersonationUtil.getProcessUserName();
  public static final String TEST_USER_1_PASSWORD = "testUser1Password";
  public static final String TEST_USER_2_PASSWORD = "testUser2Password";
  public static final String ADMIN_USER_PASSWORD = "adminUserPw";
  public static final String PROCESS_USER_PASSWORD = "processUserPw";

  public static final String ADMIN_GROUP = "admingrp";

  static {
    UserGroupInformation.createUserForTesting("testUser1", new String[]{"g1", ADMIN_GROUP});
    UserGroupInformation.createUserForTesting("testUser2", new String[]{ "g1" });
    UserGroupInformation.createUserForTesting("admin", new String[]{ ADMIN_GROUP });
  }

  @Override
  public void setup(DrillConfig drillConfig) throws DrillbitStartupException {
    // Nothing to setup.
  }

  @Override
  public void authenticate(String user, String password) throws UserAuthenticationException {

    if ("anonymous".equals(user)) {
      // Allow user "anonymous" for test framework to work.
      return;
    }

    if (
        !(PROCESS_USER.equals(user) && PROCESS_USER_PASSWORD.equals(password)) &&
     /**
      * Used in {@link org.apache.drill.exec.rpc.user.security.TestCustomUserAuthenticator}
      */
        !(TEST_USER_1.equals(user) && TEST_USER_1_PASSWORD.equals(password)) &&
        !(TEST_USER_2.equals(user) && TEST_USER_2_PASSWORD.equals(password)) &&
        !(ADMIN_USER.equals(user) && ADMIN_USER_PASSWORD.equals(password)) &&
     /**
      * Used in {@link TestInboundImpersonation}
      */
        !(OWNER.equals(user) && OWNER_PASSWORD.equals(password)) &&
        !(TARGET_NAME.equals(user) && TARGET_PASSWORD.equals(password)) &&
        !(PROXY_NAME.equals(user) && PROXY_PASSWORD.equals(password))) {
      throw new UserAuthenticationException();
    }
  }

  @Override
  public void close() throws IOException {
    // Nothing to cleanup.
  }
}
