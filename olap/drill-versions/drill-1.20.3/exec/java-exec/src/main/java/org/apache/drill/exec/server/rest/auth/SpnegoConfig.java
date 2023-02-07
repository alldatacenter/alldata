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
package org.apache.drill.exec.server.rest.auth;

import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.exceptions.DrillException;
import org.apache.drill.exec.ExecConstants;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.CommonConfigurationKeys;
import org.apache.hadoop.security.UserGroupInformation;

public class SpnegoConfig {

  // Standard Object Identifier for the SPNEGO GSS-API mechanism.
  public static final String GSS_SPNEGO_MECH_OID = "1.3.6.1.5.5.2";

  private UserGroupInformation loggedInUgi;
  private final String principal;
  private final String keytab;
  // Optional parameter
  private final String clientNameMapping;

  public SpnegoConfig(DrillConfig config) {

    keytab = config.hasPath(ExecConstants.HTTP_SPNEGO_KEYTAB) ?
        config.getString(ExecConstants.HTTP_SPNEGO_KEYTAB) :
        null;

    principal = config.hasPath(ExecConstants.HTTP_SPNEGO_PRINCIPAL) ?
        config.getString(ExecConstants.HTTP_SPNEGO_PRINCIPAL) :
        null;

    // set optional user name mapping
    clientNameMapping = config.hasPath(ExecConstants.KERBEROS_NAME_MAPPING) ?
      config.getString(ExecConstants.KERBEROS_NAME_MAPPING) :
      null;
  }

  //Reads the SPNEGO principal from the config file
  public String getSpnegoPrincipal() {
    return principal;
  }

  public void validateSpnegoConfig() throws DrillException {

    StringBuilder errorMsg = new StringBuilder();

    if (principal != null && keytab != null) {
      return;
    }

    if (principal == null) {
      errorMsg.append("\nConfiguration ");
      errorMsg.append(ExecConstants.HTTP_SPNEGO_PRINCIPAL);
      errorMsg.append(" is not found");
    }

    if (keytab == null) {
      errorMsg.append("\nConfiguration ");
      errorMsg.append(ExecConstants.HTTP_SPNEGO_KEYTAB);
      errorMsg.append(" is not found");
    }

    throw new DrillException(errorMsg.toString());
  }

  public UserGroupInformation getLoggedInUgi() throws DrillException {

    if (loggedInUgi != null) {
      return loggedInUgi;
    }
    loggedInUgi = loginAndReturnUgi();
    return loggedInUgi;
  }

  //Performs the Server login to KDC for SPNEGO
  private UserGroupInformation loginAndReturnUgi() throws DrillException {

    validateSpnegoConfig();

    UserGroupInformation ugi;
    try {
      // Check if security is not enabled and try to set the security parameter to login the principal.
      // After the login is performed reset the static UGI state.
      if (!UserGroupInformation.isSecurityEnabled()) {
        final Configuration newConfig = new Configuration();
        newConfig.set(CommonConfigurationKeys.HADOOP_SECURITY_AUTHENTICATION,
            UserGroupInformation.AuthenticationMethod.KERBEROS.toString());

        if (clientNameMapping != null) {
          newConfig.set(CommonConfigurationKeys.HADOOP_SECURITY_AUTH_TO_LOCAL, clientNameMapping);
        }

        UserGroupInformation.setConfiguration(newConfig);
        ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytab);

        // Reset the original configuration for static UGI
        UserGroupInformation.setConfiguration(new Configuration());
      } else {
        // Let's not overwrite the rules here since it might be possible that CUSTOM security is configured for
        // JDBC/ODBC with default rules. If Kerberos was enabled then the correct rules must already be set
        ugi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytab);
      }
    } catch (Exception e) {
      throw new DrillException(String.format("Login failed for %s with given keytab", principal), e);
    }
    return ugi;
  }
}
