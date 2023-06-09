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
package org.apache.drill.yarn.appMaster.http;

import java.io.IOException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.drill.common.config.DrillConfig;
import org.apache.drill.common.scanner.persistence.ScanResult;
import org.apache.drill.exec.ExecConstants;
import org.apache.drill.exec.exception.DrillbitStartupException;
import org.apache.drill.exec.rpc.user.security.UserAuthenticationException;
import org.apache.drill.exec.rpc.user.security.UserAuthenticator;
import org.apache.drill.exec.rpc.user.security.UserAuthenticatorFactory;
import org.apache.drill.exec.util.ImpersonationUtil;
import org.apache.drill.yarn.appMaster.AMWrapperException;
import org.apache.drill.yarn.core.DoYUtil;
import org.apache.drill.yarn.core.DrillOnYarnConfig;

import com.typesafe.config.Config;

/**
 * Implements the three supported AM security models: Drill,
 * hard-coded user and password, and open access.
 */

public class AMSecurityManagerImpl implements AMSecurityManager {
  private static final Log LOG = LogFactory.getLog(AMSecurityManagerImpl.class);

  /**
   * Thin layer around the Drill authentication system to adapt from
   * Drill-on-YARN's environment to that expected by the Drill classes.
   */
  private static class DrillSecurityManager implements AMSecurityManager {
    private UserAuthenticator authenticator;

    @Override
    public void init() {
      try {
        DrillOnYarnConfig doyConfig = DrillOnYarnConfig.instance();
        DrillConfig config = doyConfig.getDrillConfig();
        ScanResult classpathScan = doyConfig.getClassPathScan();
        if (config.getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED)) {
          authenticator = UserAuthenticatorFactory.createAuthenticator(config,
              classpathScan);
        } else {
          authenticator = null;
        }
      } catch (DrillbitStartupException e) {
        LOG.info("Authentication initialization failed", e);
        throw new AMWrapperException("Security init failed", e);
      }
    }

    @Override
    public boolean login(String user, String password) {
      if (authenticator == null) {
        return true;
      }
      try {
        authenticator.authenticate(user, password);
      } catch (UserAuthenticationException e) {
        LOG.info("Authentication failed for user " + user, e);
        return false;
      }
      return ImpersonationUtil.getProcessUserName().equals(user);
    }

    @Override
    public void close() {
      try {
        if (authenticator != null) {
          authenticator.close();
        }
      } catch (IOException e) {
        LOG.info("Ignoring error on authenticator close", e);
      }
    }

    @Override
    public boolean requiresLogin() {
      return authenticator != null;
    }
  }

  /**
   * Simple security manager: user name and password reside in the DoY config
   * file.
   */

  private static class SimpleSecurityManager implements AMSecurityManager {

    private String userName;
    private String password;

    @Override
    public void init() {
      Config config = DrillOnYarnConfig.config();
      userName = config.getString(DrillOnYarnConfig.HTTP_USER_NAME);
      password = config.getString(DrillOnYarnConfig.HTTP_PASSWORD);
      if (DoYUtil.isBlank(userName)) {
        LOG.warn("Simple HTTP authentication is enabled, but "
            + DrillOnYarnConfig.HTTP_USER_NAME + " is blank.");
      }
      if (DoYUtil.isBlank(userName)) {
        LOG.warn("Simple HTTP authentication is enabled, but "
            + DrillOnYarnConfig.HTTP_PASSWORD + " is blank.");
      }
    }

    @Override
    public boolean requiresLogin() {
      return !DoYUtil.isBlank(userName);
    }

    @Override
    public boolean login(String user, String pwd) {
      if (!requiresLogin()) {
        return true;
      }
      boolean ok = userName.equals(user) && password.equals(pwd);
      if (!ok) {
        LOG.info(
            "Failed login attempt with simple authorization for user " + user);
      }
      return ok;
    }

    @Override
    public void close() {
      // Nothing to do
    }

  }

  private static AMSecurityManagerImpl instance;

  private AMSecurityManager managerImpl;

  private AMSecurityManagerImpl() {
  }

  public static void setup() {
    instance = new AMSecurityManagerImpl();
    instance.init();
  }

  /**
   * Look at the DoY config file to decide which security system (if any) to
   * use.
   */

  @Override
  public void init() {
    Config config = DrillOnYarnConfig.config();
    String authType = config.getString(DrillOnYarnConfig.HTTP_AUTH_TYPE);
    if (DrillOnYarnConfig.AUTH_TYPE_DRILL.equals(authType)) {
      // Drill authentication. Requires both DoY to select Drill
      // auth, and for Drill's auth to be enabled.
      if(config.getBoolean(ExecConstants.USER_AUTHENTICATION_ENABLED)) {
        managerImpl = new DrillSecurityManager();
        managerImpl.init();
      }
    } else if (DrillOnYarnConfig.AUTH_TYPE_SIMPLE.equals(authType)) {
      managerImpl = new SimpleSecurityManager();
      managerImpl.init();
    } else if (DoYUtil.isBlank(authType)
        || DrillOnYarnConfig.AUTH_TYPE_NONE.equals(authType)) {
    } else {
      LOG.error("Unrecognized authorization type for "
          + DrillOnYarnConfig.HTTP_AUTH_TYPE + ": " + authType
          + " - assuming no auth.");
    }
  }

  @Override
  public boolean login(String user, String password) {
    if (managerImpl == null) {
      return true;
    }
    return managerImpl.login(user, password);
  }

  @Override
  public void close() {
    if (managerImpl != null) {
      managerImpl.close();
      managerImpl = null;
    }
  }

  @Override
  public boolean requiresLogin() {
    return managerImpl != null;
  }

  public static AMSecurityManager instance() {
    return instance;
  }

  public static boolean isEnabled() {
    return instance != null && instance.managerImpl != null;
  }
}
