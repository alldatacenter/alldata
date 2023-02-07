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
package org.apache.drill.exec.rpc.user.security;

import net.sf.jpam.Pam;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;

import java.io.IOException;
import java.util.List;

/**
 * Implement {@link org.apache.drill.exec.rpc.user.security.UserAuthenticator} based on Pluggable Authentication
 * Module (PAM) configuration. Configure the PAM profiles using "drill.exec.security.user.auth.pam_profiles" BOOT
 * option. Ex. value  <i>[ "login", "sudo" ]</i> (value is an array of strings).
 */
@UserAuthenticatorTemplate(type = "pam")
public class PamUserAuthenticator implements UserAuthenticator {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(PamUserAuthenticator.class);

  private List<String> profiles;

  @Override
  public void setup(DrillConfig drillConfig) throws DrillbitStartupException {
    profiles = drillConfig.getStringList(ExecConstants.PAM_AUTHENTICATOR_PROFILES);

    // Create a JPAM object so that it triggers loading of native "jpamlib" needed. Issues in loading/finding native
    // "jpamlib" will be found it Drillbit start rather than when authenticating the first user.
    try {
      new Pam();
    } catch(LinkageError e) {
      final String errMsg = "Problem in finding the native library of JPAM (Pluggable Authenticator Module API). " +
          "Make sure to set Drillbit JVM option 'java.library.path' to point to the directory where the native " +
          "JPAM exists.";
      logger.error(errMsg, e);
      throw new DrillbitStartupException(errMsg + ":" + e.getMessage(), e);
    }
  }

  @Override
  public void authenticate(String user, String password) throws UserAuthenticationException {
    for (String pamProfile : profiles) {
      Pam pam = new Pam(pamProfile);
      if (!pam.authenticateSuccessful(user, password)) {
        throw new UserAuthenticationException(String.format("PAM profile '%s' validation failed for user %s",
            pamProfile, user));
      }
    }
  }

  @Override
  public void close() throws IOException {
    // No-op as no resources are occupied by PAM authenticator.
  }
}
