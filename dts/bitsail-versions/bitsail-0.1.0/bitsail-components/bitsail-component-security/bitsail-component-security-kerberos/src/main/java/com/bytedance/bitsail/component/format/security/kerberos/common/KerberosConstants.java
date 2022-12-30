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

package com.bytedance.bitsail.component.format.security.kerberos.common;

public class KerberosConstants {
  public static final String SYSTEM_ENV_JAVA_SECURITY_AUTH_LOGIN_CONFIG = "java.security.auth.login.config";

  public static final String SYSTEM_ENV_KRB5_CONF_PATH = "java.security.krb5.conf";
  public static final String USE_SUBJECT_CREDS_ONLY = "javax.security.auth.useSubjectCredsOnly";
  public static final String HADOOP_AUTH_KEY = "hadoop.security.authentication";

  public static final String OPTION_KEYTAB = "keyTab";
  public static final String OPTION_USE_KEY_TAB = "useKeytab";
  public static final String OPTION_CREDS_TYPE = "credsType";
  public static final String OPTION_DO_NOT_PROMPT = "doNotPrompt";
  public static final String OPTION_STORE_KEY = "storeKey";
  public static final String OPTION_PRINCIPAL = "principal";
  public static final String OPTION_REFRESH_KRB5_CONFIG = "refreshKrb5Config";

  public static final String VALUE_CREDS_TYPE_BOTH = "both";
  public static final String VALUE_BOOLEAN_TRUE = "true";

  public static final String KRB5_LOGIN_MODULE_SUN = "com.sun.security.auth.module.Krb5LoginModule";
  public static final String KRB5_LOGIN_MODULE_IBM = "com.ibm.security.auth.module.Krb5LoginModule";

  public static final String TMP_FILEPATH_KRB5_CONTENT = "/tmp/kerbros-bitsail/krb5.conf";
  public static final String TMP_FILEPATH_KEYTAB_CONTENT = "/tmp/kerbros-bitsail/principal.keytab";
  public static final String TMP_FILEPATH_JAAS_CONTENT = "/tmp/kerberos-bitsail/jaas.conf";

  public static final String KERBEROS_AUTH_MODE_APP = "app";
  public static final String KERBEROS_AUTH_MODE_CONNECTOR = "connector";
}
