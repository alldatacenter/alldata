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

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.common.util.Preconditions;
import com.bytedance.bitsail.component.format.security.kerberos.common.KerberosConstants;
import com.bytedance.bitsail.component.format.security.kerberos.option.KerberosOptions;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class HadoopSecurityModule extends KerberosAbstractSecurityModule {

  private static final Logger LOG = LoggerFactory.getLogger(HadoopSecurityModule.class);

  private Boolean useSubjectCreditsOnly;
  private UserGroupInformation userGroupInformation;

  @Override
  public void initializeModule(BitSailConfiguration securityConfiguration) {
    super.initializeModule(securityConfiguration);
    this.useSubjectCreditsOnly = securityConfiguration
        .get(KerberosOptions.KERBEROS_KRB5_USE_SUBJECT_CREDITS_ONLY);
  }

  public void login() throws IOException {
    if (!securityConfiguration.get(KerberosOptions.KERBEROS_ENABLE)) {
      LOG.info("Hadoop module disabled.");
      return;
    }
    Preconditions.checkNotNull(principal, "UserGroupInformationUtils: principal cannot be null");
    Preconditions.checkNotNull(keytabPath, "UserGroupInformationUtils: keytabPath cannot be null");
    Preconditions.checkNotNull(krb5confPath, "UserGroupInformationUtils: krb5confPath cannot be null");

    // refresh realm according to current krb5 conf
    System.setProperty(KerberosConstants.SYSTEM_ENV_KRB5_CONF_PATH, krb5confPath);
    refreshConfig();

    if (Objects.nonNull(useSubjectCreditsOnly)) {
      System.setProperty(KerberosConstants.USE_SUBJECT_CREDS_ONLY, useSubjectCreditsOnly.toString());
    }
    Configuration configuration = new Configuration();

    Map<String, String> kerberosHadoopConf = securityConfiguration.getUnNecessaryOption(KerberosOptions.KERBEROS_HADOOP_CONF,
        new HashMap<>());
    kerberosHadoopConf.forEach(configuration::set);
    configuration.set(KerberosConstants.HADOOP_AUTH_KEY, "Kerberos");

    UserGroupInformation.reset();
    UserGroupInformation.setConfiguration(configuration);

    userGroupInformation = UserGroupInformation.loginUserFromKeytabAndReturnUGI(principal, keytabPath);
    LOG.info("Successfully login, login user: {}.", userGroupInformation);
  }

  @Override
  public void logout() {
    UserGroupInformation.reset();
  }
}
