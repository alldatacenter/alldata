/**
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

module.exports = [
  {
    "category": "KDC",
    "filename": "kerberos-env.xml",
    "index": 0,
    "name": "kdc_type",
    "serviceName": "KERBEROS"
  },
  {
    "category": "KDC",
    "displayType": "supportTextConnection",
    "filename": "kerberos-env.xml",
    "index": 1,
    "name": "kdc_hosts",
    "serviceName": "KERBEROS"
  },
  {
    "category": "KDC",
    "filename": "kerberos-env.xml",
    "index": 2,
    "name": "realm",
    "serviceName": "KERBEROS"
  },
  {
    "category": "KDC",
    "filename": "kerberos-env.xml",
    "index": 3,
    "name": "ldap_url",
    "serviceName": "KERBEROS"
  },
  {
    "category": "KDC",
    "filename": "kerberos-env.xml",
    "index": 4,
    "name": "container_dn",
    "serviceName": "KERBEROS"
  },
  {
    "category": "KDC",
    "filename": "krb5-conf.xml",
    "index": 5,
    "name": "domains",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 0,
    "name": "manage_identities",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 1,
    "name": "install_packages",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 2,
    "name": "executable_search_paths",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 3,
    "name": "encryption_types",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 4,
    "name": "password_length",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 5,
    "name": "password_min_lowercase_letters",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 6,
    "name": "password_min_uppercase_letters",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 7,
    "name": "password_min_digits",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 8,
    "name": "password_min_punctuation",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 9,
    "name": "password_min_whitespace",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 10,
    "name": "service_check_principal_name",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 11,
    "name": "ad_create_attributes_template",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 12,
    "name": "kdc_create_attributes",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 13,
    "name": "case_insensitive_username_rules",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 14,
    "name": "manage_auth_to_local",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced kerberos-env",
    "filename": "kerberos-env.xml",
    "index": 15,
    "name": "group",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Kadmin",
    "filename": "kerberos-env.xml",
    "index": 0,
    "name": "admin_server_host",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced krb5-conf",
    "dependentConfigPattern": "CATEGORY",
    "filename": "krb5-conf.xml",
    "index": 0,
    "name": "manage_krb5_conf",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced krb5-conf",
    "filename": "krb5-conf.xml",
    "index": 1,
    "name": "conf_dir",
    "serviceName": "KERBEROS"
  },
  {
    "category": "Advanced krb5-conf",
    "filename": "krb5-conf.xml",
    "index": 2,
    "name": "content",
    "serviceName": "KERBEROS"
  }
];