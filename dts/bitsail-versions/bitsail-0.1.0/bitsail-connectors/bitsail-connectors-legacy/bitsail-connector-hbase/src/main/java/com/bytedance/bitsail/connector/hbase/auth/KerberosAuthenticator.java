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

package com.bytedance.bitsail.connector.hbase.auth;

import com.bytedance.bitsail.common.configuration.BitSailConfiguration;
import com.bytedance.bitsail.component.format.security.kerberos.option.KerberosOptions;
import com.bytedance.bitsail.component.format.security.kerberos.security.HadoopSecurityModule;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.hadoop.security.UserGroupInformation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sun.security.krb5.internal.ktab.KeyTab;
import sun.security.krb5.internal.ktab.KeyTabEntry;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.AUTHENTICATION_TYPE;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_FS_HDFS_IMPL_DISABLE_CACHE;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_HBASE_SECURITY_AUTHENTICATION;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_HBASE_SECURITY_AUTHORIZATION;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_JAVA_SECURITY_KRB5_CONF;
import static com.bytedance.bitsail.connector.hbase.constant.HBaseConstants.KEY_PRINCIPAL;

public class KerberosAuthenticator {
  private static final Logger LOG = LoggerFactory.getLogger(KerberosAuthenticator.class);

  /**
   * Enable kerberos only if<br/>
   * 1. hbase.security.authorization=true<br/>
   * 2. hbase.security.authentication=kerberos<br/>
   * @param hbaseConfigMap Hbase configurations.
   * @return If enable kerberos.
   */
  public static boolean openKerberos(Map<String, Object> hbaseConfigMap) {
    return MapUtils.getBooleanValue(hbaseConfigMap, KEY_HBASE_SECURITY_AUTHORIZATION) &&
        AUTHENTICATION_TYPE.equalsIgnoreCase(MapUtils.getString(hbaseConfigMap, KEY_HBASE_SECURITY_AUTHENTICATION));
  }

  public static UserGroupInformation getUgi(Map<String, Object> hbaseConfigMap) throws IOException {
    // *** step0: write content to local file ***
    KerberosFileHandler.loadConfContent(hbaseConfigMap);
    KerberosFileHandler.adjustHbaseConf(hbaseConfigMap);

    // *** step1: initialize parameters ***
    String keytabFileName = KerberosFileHandler.getPrincipalFileName(hbaseConfigMap);
    keytabFileName = KerberosFileHandler.loadFile(hbaseConfigMap, keytabFileName);
    String principal = getPrincipal(hbaseConfigMap, keytabFileName);
    KerberosFileHandler.loadKrb5Conf(hbaseConfigMap);
    String krb5ConfPath = MapUtils.getString(hbaseConfigMap, KEY_JAVA_SECURITY_KRB5_CONF);

    Map<String, String> kerberosHadoopConf = new HashMap<>();
    if (Objects.nonNull(hbaseConfigMap)) {
      hbaseConfigMap.forEach((k, v) -> kerberosHadoopConf.put(k, v.toString()));
    }
    kerberosHadoopConf.put(KEY_FS_HDFS_IMPL_DISABLE_CACHE, "true");

    BitSailConfiguration securityConf = BitSailConfiguration.newDefault();
    securityConf.set(KerberosOptions.KERBEROS_PRINCIPAL, principal);
    securityConf.set(KerberosOptions.KERBEROS_KEYTAB_PATH, keytabFileName);
    securityConf.set(KerberosOptions.KERBEROS_KRB5_CONF_PATH, krb5ConfPath);
    securityConf.set(KerberosOptions.KERBEROS_HADOOP_CONF, kerberosHadoopConf);
    securityConf.set(KerberosOptions.KERBEROS_ENABLE, true);

    // *** step2: login hadoop security module ***
    HadoopSecurityModule securityModule = new HadoopSecurityModule();
    securityModule.initializeModule(securityConf);
    securityModule.login();

    return UserGroupInformation.getCurrentUser();
  }

  /**
   * Find principal from configuration of keytab file.
   * @param configMap HBase configuration.
   * @param keytabPath Keytab file.
   * @return Principal of the first entry in the keytab.
   */
  public static String getPrincipal(Map<String, Object> configMap, String keytabPath) {
    String principal = MapUtils.getString(configMap, KEY_PRINCIPAL);
    if (StringUtils.isNotEmpty(principal)) {
      return principal;
    }

    KeyTab keyTab = KeyTab.getInstance(keytabPath);
    for (KeyTabEntry entry : keyTab.getEntries()) {
      principal = entry.getService().getName();
      LOG.info("parse principal:{} from keytab:{}", principal, keytabPath);
      return principal;
    }
    return null;
  }

  /**
   * Check if the cluster enable kerberos.
   * todo: Should check if sys configuration enables kerberos.
   */
  public static boolean enableKerberosConfig() {
    return false;
  }

  /**
   * Add kerberos content in sys configuration into hbase configuration.
   */
  public static void addHBaseKerberosConf(Map<String, Object> optionalConfig) {
    // todo: Fill the code after enableKerberosConfig is completed.
  }
}
