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

import java.io.File;
import java.util.Map;

import javax.security.auth.login.AppConfigurationEntry;
import javax.security.auth.login.LoginContext;
import javax.security.auth.login.LoginException;

import org.apache.ambari.server.AmbariException;
import org.apache.ambari.server.configuration.Configuration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Inject;

public class KerberosChecker {

  static final String HTTP_SPNEGO_STANDARD_ENTRY =
      "com.sun.security.jgss.krb5.initiate";
  private static final String KRB5_LOGIN_MODULE =
      "com.sun.security.auth.module.Krb5LoginModule";
  public static final String JAVA_SECURITY_AUTH_LOGIN_CONFIG =
      "java.security.auth.login.config";

  private static final Logger LOG = LoggerFactory.getLogger(KerberosChecker.class);

  @Inject
  static Configuration config;

  /**
   * Used to help create new LoginContext instances
   */
  @Inject
  static LoginContextHelper loginContextHelper;

  /**
   * Checks Ambari Server with a Kerberos principal and keytab to allow views
   * to authenticate via SPNEGO against cluster components.
   *
   * @throws AmbariException
   */
  public static void checkJaasConfiguration() throws AmbariException {

    if (config.isKerberosJaasConfigurationCheckEnabled()) {
      LOG.info("Checking Ambari Server Kerberos credentials.");

      String jaasConfPath = System.getProperty(JAVA_SECURITY_AUTH_LOGIN_CONFIG);

      javax.security.auth.login.Configuration jaasConf =
          javax.security.auth.login.Configuration.getConfiguration();

      AppConfigurationEntry[] jaasConfEntries =
          jaasConf.getAppConfigurationEntry(HTTP_SPNEGO_STANDARD_ENTRY);

      if (jaasConfEntries == null) {
        LOG.warn("Can't find " + HTTP_SPNEGO_STANDARD_ENTRY + " entry in " +
            jaasConfPath);
      } else {
        boolean krb5LoginModulePresent = false;
        for (AppConfigurationEntry ace : jaasConfEntries) {
          if (KRB5_LOGIN_MODULE.equals(ace.getLoginModuleName())) {
            krb5LoginModulePresent = true;
            Map<String, ?> options = ace.getOptions();
            if ((options != null)) {
              if (options.containsKey("keyTab")) {
                String keytabPath = (String) options.get("keyTab");
                File keytabFile = new File(keytabPath);
                if (!keytabFile.exists()) {
                  LOG.warn(keytabPath + " doesn't exist.");
                } else if (!keytabFile.canRead()) {
                  LOG.warn("Unable to read " + keytabPath +
                      " Please check the file access permissions for user " +
                      System.getProperty("user.name"));
                }
              } else {
                LOG.warn("Can't find keyTab option in " + KRB5_LOGIN_MODULE +
                    " module of " + HTTP_SPNEGO_STANDARD_ENTRY + " entry in " +
                    jaasConfPath);
              }

              if (!options.containsKey("principal")) {
                LOG.warn("Can't find principal option in " + KRB5_LOGIN_MODULE +
                    " module of " + HTTP_SPNEGO_STANDARD_ENTRY + " entry in " +
                    jaasConfPath);
              }
            }
          }
        }
        if (!krb5LoginModulePresent) {
          LOG.warn("Can't find " + KRB5_LOGIN_MODULE + " module in " +
              HTTP_SPNEGO_STANDARD_ENTRY + " entry in " + jaasConfPath);
        }
      }

      try {
        LoginContext loginContext = loginContextHelper.createLoginContext(HTTP_SPNEGO_STANDARD_ENTRY);

        loginContext.login();
        loginContext.logout();
      } catch (LoginException le) {
        LOG.error(le.getMessage());
        throw new AmbariException(
            "Ambari Server Kerberos credentials check failed. \n" +
                "Check KDC availability and JAAS configuration in " + jaasConfPath);
      }

      LOG.info("Ambari Server Kerberos credentials check passed.");
    } else {
      LOG.info("Skipping Ambari Server Kerberos credentials check.");
    }
  }

}
