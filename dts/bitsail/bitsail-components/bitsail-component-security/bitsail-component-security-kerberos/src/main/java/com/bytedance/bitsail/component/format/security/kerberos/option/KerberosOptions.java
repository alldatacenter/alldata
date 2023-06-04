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

package com.bytedance.bitsail.component.format.security.kerberos.option;

import com.bytedance.bitsail.common.option.CommonOptions;
import com.bytedance.bitsail.common.option.ConfigOption;

import com.alibaba.fastjson.TypeReference;

import java.util.Map;

import static com.bytedance.bitsail.common.option.ConfigOptions.key;

public interface KerberosOptions extends CommonOptions {

  String SYS_KERBEROS = "sys.kerberos.";

  ConfigOption<Boolean> KERBEROS_ENABLE =
      key(SYS_KERBEROS + "enable")
          .defaultValue(false);

  ConfigOption<String> KERBEROS_KEYTAB_PATH =
      key(SYS_KERBEROS + "keytab_path")
          .noDefaultValue(String.class);

  ConfigOption<String> KERBEROS_PRINCIPAL =
      key(SYS_KERBEROS + "principal")
          .noDefaultValue(String.class);

  ConfigOption<String> KERBEROS_KRB5_CONF_PATH =
      key(SYS_KERBEROS + "krb5_conf_path")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> KERBEROS_KRB5_USE_SUBJECT_CREDITS_ONLY =
      key(SYS_KERBEROS + "use_subject_credits_only")
          .noDefaultValue(Boolean.class);

  ConfigOption<Map<String, String>> KERBEROS_HADOOP_CONF =
      key(SYS_KERBEROS + "hadoop_conf")
          .onlyReference(new TypeReference<Map<String, String>>() {
          });

  ConfigOption<String> JAAS_CONF_PATH =
      key(SYS_KERBEROS + "jaas_conf_path")
          .noDefaultValue(String.class);

  ConfigOption<Boolean> ENABLE_JAAS_CONFIG_FILE_EXIST_CHECK =
      key(SYS_KERBEROS + "enable_jaas_config_file_check")
          .defaultValue(false);

  ConfigOption<Map<String, String>> KEYTAB_ENTRY_PROPERTIES =
      key(SYS_KERBEROS + "keytab_entry_properties")
          .onlyReference(new TypeReference<Map<String, String>>() {
          });

}
