/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.bytedance.bitsail.client.entry.security;

import com.bytedance.bitsail.base.extension.SecurityModule;
import com.bytedance.bitsail.client.api.command.BaseCommandArgs;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.component.format.security.kerberos.option.KerberosOptions;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.concurrent.Callable;

public class SecurityContextFactory {
  private static final Logger LOG = LoggerFactory.getLogger(SecurityContextFactory.class);

  private final BitSailConfiguration securityConfiguration;
  private final List<SecurityModule> securityModules;

  public static SecurityContextFactory load(BitSailConfiguration sysConfiguration,
                                            BaseCommandArgs baseCommandArgs) {
    return new SecurityContextFactory(sysConfiguration, baseCommandArgs);
  }

  public SecurityContextFactory(BitSailConfiguration sysConfiguration, BaseCommandArgs baseCommandArgs) {
    this.securityConfiguration = mergeSecurityConfiguration(sysConfiguration, baseCommandArgs);
    this.securityModules = loadSecurityModules();
  }

  private BitSailConfiguration mergeSecurityConfiguration(BitSailConfiguration sysConfiguration,
                                                          BaseCommandArgs baseCommandArgs) {
    sysConfiguration.set(KerberosOptions.KERBEROS_ENABLE, baseCommandArgs.isEnableKerberos());
    sysConfiguration.set(KerberosOptions.KERBEROS_KEYTAB_PATH, baseCommandArgs.getKeytabPath());
    sysConfiguration.set(KerberosOptions.KERBEROS_KRB5_CONF_PATH, baseCommandArgs.getKrb5ConfPath());
    sysConfiguration.set(KerberosOptions.KERBEROS_PRINCIPAL, baseCommandArgs.getPrincipal());
    return sysConfiguration;
  }

  private List<SecurityModule> loadSecurityModules() {
    List<SecurityModule> securityModules = new ArrayList<>();
    for (SecurityModule module : ServiceLoader.load(SecurityModule.class)) {
      securityModules.add(module);
    }
    return securityModules;
  }

  public <T> T doAs(Callable<T> callable) throws Exception {
    for (SecurityModule securityModule : securityModules) {
      securityModule.initializeModule(securityConfiguration);
      LOG.info("Module {} start login.", securityModule.getClass().getSimpleName());
      securityModule.login();
    }
    return callable.call();
  }
}
