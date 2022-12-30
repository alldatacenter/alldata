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

package com.bytedance.bitsail.component.format.security.kerberos.security;

import com.bytedance.bitsail.base.extension.SecurityModule;
import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.component.format.security.kerberos.option.KerberosOptions;

import org.apache.hadoop.security.authentication.util.KerberosName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.Config;
import sun.security.krb5.KrbException;

public abstract class KerberosAbstractSecurityModule implements SecurityModule {
  private static final Logger LOG = LoggerFactory.getLogger(KerberosAbstractSecurityModule.class);
  protected BitSailConfiguration securityConfiguration;

  protected String principal;
  protected String keytabPath;
  protected String krb5confPath;

  @Override
  public void initializeModule(BitSailConfiguration securityConfiguration) {
    this.securityConfiguration = securityConfiguration;
    this.principal = securityConfiguration.get(KerberosOptions.KERBEROS_PRINCIPAL);
    this.keytabPath = securityConfiguration.get(KerberosOptions.KERBEROS_KEYTAB_PATH);
    this.krb5confPath = securityConfiguration.get(KerberosOptions.KERBEROS_KRB5_CONF_PATH);

    LOG.info("Kerberos principal={}, keytabPath={}, krb5confPath={}", principal, keytabPath, krb5confPath);
    LOG.info("Kerberos security module initialized.");
  }

  protected void refreshConfig() {
    try {
      Config.refresh();
      KerberosName.resetDefaultRealm();
    } catch (KrbException e) {
      LOG.warn("Failed to refresh krb5 config or reset default realm, current default realm {} will be used.",
          KerberosName.getDefaultRealm(), e);
    }
  }
}
