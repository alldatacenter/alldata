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

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.jvnet.libpam.PAM;
import org.jvnet.libpam.PAMException;

import java.io.IOException;
import java.util.List;

/**
 * Implement {@link org.apache.drill.exec.rpc.user.security.UserAuthenticator} based on Pluggable Authentication
 * Module (PAM) configuration. Configure the PAM profiles using "drill.exec.security.user.auth.pam_profiles" BOOT
 * option. Ex. value  <i>[ "login", "sudo" ]</i> (value is an array of strings).
 */
@UserAuthenticatorTemplate(type = "pam4j")
public class Pam4jUserAuthenticator implements UserAuthenticator {
  private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(Pam4jUserAuthenticator.class);

  private List<String> profiles;

  @Override
  public void setup(DrillConfig drillConfig) throws DrillbitStartupException {
    profiles = drillConfig.getStringList(ExecConstants.PAM_AUTHENTICATOR_PROFILES);
  }

  @Override
  public void authenticate(String user, String password) throws UserAuthenticationException {
    for (String profile : profiles) {
      PAM pam = null;

      try {
        pam = new PAM(profile);
        pam.authenticate(user, password);
      } catch (PAMException ex) {
        logger.error("PAM auth failed for user: {} against {} profile. Exception: {}", user, profile, ex.getMessage());
        throw new UserAuthenticationException(String.format("PAM auth failed for user: %s using profile: %s",
            user, profile));
      } finally {
        if (pam != null) {
          pam.dispose();
        }
      }

      // No need to check for null unixUser as in case of failure we will not reach here.
      logger.trace("PAM authentication was successful for user: {} using profile: {}", user, profile);
    }
  }

  @Override
  public void close() throws IOException {
    // No-op as no resources are occupied by PAM authenticator.
  }
}