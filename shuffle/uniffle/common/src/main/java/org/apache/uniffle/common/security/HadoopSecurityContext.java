/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.uniffle.common.security;

import java.io.IOException;
import java.security.PrivilegedExceptionAction;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.uniffle.common.util.ThreadUtils;

public class HadoopSecurityContext implements SecurityContext {
  private static final Logger LOGGER = LoggerFactory.getLogger(HadoopSecurityContext.class);
  private static final String KRB5_CONF_KEY = "java.security.krb5.conf";

  private UserGroupInformation loginUgi;
  private ScheduledExecutorService refreshScheduledExecutor;

  public HadoopSecurityContext(
      String krb5ConfPath,
      String keytabFile,
      String principal,
      long refreshIntervalSec) throws Exception {
    if (StringUtils.isEmpty(keytabFile)) {
      throw new IllegalArgumentException("KeytabFilePath must be not null or empty");
    }
    if (StringUtils.isEmpty(principal)) {
      throw new IllegalArgumentException("principal must be not null or empty");
    }
    if (refreshIntervalSec <= 0) {
      throw new IllegalArgumentException("refreshIntervalSec must be not negative");
    }

    if (StringUtils.isNotEmpty(krb5ConfPath)) {
      System.setProperty(KRB5_CONF_KEY, krb5ConfPath);
      sun.security.krb5.Config.refresh();
    }

    Configuration conf = new Configuration(false);
    conf.set("hadoop.security.authentication", "kerberos");
    UserGroupInformation.setConfiguration(conf);

    this.loginUgi = UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytabFile);

    LOGGER.info("Got Kerberos ticket, keytab [{}], principal [{}], user [{}]",
        keytabFile, principal, loginUgi.getShortUserName());

    refreshScheduledExecutor = Executors.newSingleThreadScheduledExecutor(
        ThreadUtils.getThreadFactory("Kerberos-refresh-%d")
    );
    refreshScheduledExecutor.scheduleAtFixedRate(
        this::authRefresh,
        refreshIntervalSec,
        refreshIntervalSec,
        TimeUnit.SECONDS);
  }

  private void authRefresh() {
    try {
      LOGGER.info("Renewing kerberos token.");
      loginUgi.checkTGTAndReloginFromKeytab();
    } catch (Throwable t) {
      LOGGER.error("Error in token renewal task: ", t);
    }
  }

  @Override
  public <T> T runSecured(String user, Callable<T> securedCallable) throws Exception {
    if (StringUtils.isEmpty(user)) {
      throw new Exception("User must be not null or empty");
    }

    // Run with the proxy user.
    if (!user.equals(loginUgi.getShortUserName())) {
      return executeWithUgiWrapper(
          UserGroupInformation.createProxyUser(user, loginUgi),
          securedCallable
      );
    }

    // Run with the current login user.
    return executeWithUgiWrapper(loginUgi, securedCallable);
  }

  @Override
  public String getContextLoginUser() {
    return loginUgi.getShortUserName();
  }

  private <T> T executeWithUgiWrapper(UserGroupInformation ugi, Callable<T> callable) throws Exception {
    return ugi.doAs((PrivilegedExceptionAction<T>) callable::call);
  }

  @Override
  public void close() throws IOException {
    if (refreshScheduledExecutor != null) {
      refreshScheduledExecutor.shutdown();
    }
  }
}
