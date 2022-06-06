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
    "category": "RANGER_ADMIN",
    "filename": "ranger-env.xml",
    "index": 0,
    "name": "ranger_admin_username",
    "serviceName": "RANGER"
  },
  {
    "category": "RANGER_ADMIN",
    "filename": "ranger-env.xml",
    "index": 1,
    "name": "ranger_admin_password",
    "serviceName": "RANGER"
  },
  {
    "category": "RANGER_ADMIN",
    "filename": "ranger-env.xml",
    "index": 2,
    "name": "rangerusersync_user_password",
    "serviceName": "RANGER"
  },
  {
    "category": "RANGER_ADMIN",
    "filename": "ranger-env.xml",
    "index": 3,
    "name": "rangertagsync_user_password",
    "serviceName": "RANGER"
  },
  {
    "category": "RANGER_ADMIN",
    "filename": "ranger-env.xml",
    "index": 4,
    "name": "keyadmin_user_password",
    "serviceName": "RANGER"
  },
  {
    "category": "RangerSettings",
    "filename": "ranger-env.xml",
    "name": "ranger_user",
    "serviceName": "RANGER"
  },
  {
    "category": "RangerSettings",
    "filename": "ranger-env.xml",
    "name": "ranger_group",
    "serviceName": "RANGER"
  },
  {
    "category": "RangerSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.externalurl",
    "serviceName": "RANGER"
  },
  {
    "category": "RangerSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.service.http.enabled",
    "serviceName": "RANGER"
  },
  {
    "category": "RangerSettings",
    "displayType": "radio button",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.authentication.method",
    "options": [
      {
        "displayName": "LDAP",
        "foreignKeys": [
          "ranger.ldap.group.roleattribute",
          "ranger.ldap.url",
          "ranger.ldap.user.dnpattern",
          "ranger.ldap.base.dn",
          "ranger.ldap.bind.dn",
          "ranger.ldap.bind.password",
          "ranger.ldap.referral",
          "ranger.ldap.user.searchfilter",
          "ranger.ldap.group.searchbase",
          "ranger.ldap.group.searchfilter"
        ]
      },
      {
        "displayName": "ACTIVE_DIRECTORY",
        "foreignKeys": [
          "ranger.ldap.ad.domain",
          "ranger.ldap.ad.url",
          "ranger.ldap.ad.base.dn",
          "ranger.ldap.ad.bind.dn",
          "ranger.ldap.ad.bind.password",
          "ranger.ldap.ad.referral",
          "ranger.ldap.ad.user.searchfilter"
        ]
      },
      {
        "displayName": "UNIX",
        "foreignKeys": [
          "ranger.unixauth.service.port",
          "ranger.unixauth.service.hostname",
          "ranger.unixauth.remote.login.enabled"
        ]
      },
      {
        "displayName": "PAM"
      },
      {
        "displayName": "NONE"
      }
    ],
    "radioName": "authentication-method",
    "serviceName": "RANGER"
  },
  {
    "category": "RangerSettings",
    "filename": "admin-properties.xml",
    "name": "policymgr_external_url",
    "serviceName": "RANGER"
  },
  {
    "category": "UnixAuthenticationSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.unixauth.remote.login.enabled",
    "serviceName": "RANGER"
  },
  {
    "category": "UnixAuthenticationSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.unixauth.service.hostname",
    "serviceName": "RANGER"
  },
  {
    "category": "UnixAuthenticationSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.unixauth.service.port",
    "serviceName": "RANGER"
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.url",
    "serviceName": "RANGER",
    "index": 1
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.user.dnpattern",
    "serviceName": "RANGER"
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.group.roleattribute",
    "serviceName": "RANGER"
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.base.dn",
    "serviceName": "RANGER"
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.bind.dn",
    "serviceName": "RANGER",
    "index": 2
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.bind.password",
    "serviceName": "RANGER",
    "index": 3
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.referral",
    "serviceName": "RANGER"
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.user.searchfilter",
    "serviceName": "RANGER",
    "index": 4
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.group.searchbase",
    "serviceName": "RANGER",
    "index": 5
  },
  {
    "category": "LDAPSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.group.searchfilter",
    "serviceName": "RANGER",
    "index": 6
  },
  {
    "category": "KnoxSSOSettings",
    "filename": "ranger-admin-site.xml",
    "index": 2,
    "name": "ranger.sso.providerurl",
    "serviceName": "RANGER"
  },
  {
    "category": "KnoxSSOSettings",
    "filename": "ranger-admin-site.xml",
    "index": 3,
    "name": "ranger.sso.publicKey",
    "serviceName": "RANGER"
  },
  {
    "category": "KnoxSSOSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.sso.cookiename",
    "serviceName": "RANGER"
  },
  {
    "category": "KnoxSSOSettings",
    "filename": "ranger-admin-site.xml",
    "index": 1,
    "name": "ranger.sso.enabled",
    "serviceName": "RANGER"
  },
  {
    "category": "KnoxSSOSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.sso.query.param.originalurl",
    "serviceName": "RANGER"
  },
  {
    "category": "KnoxSSOSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.sso.browser.useragent",
    "serviceName": "RANGER"
  },
  {
    "category": "ADSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.ad.domain",
    "index": 4,
    "serviceName": "RANGER"
  },
  {
    "category": "ADSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.ad.url",
    "index": 1,
    "serviceName": "RANGER"
  },
  {
    "category": "ADSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.ad.base.dn",
    "serviceName": "RANGER"
  },
  {
    "category": "ADSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.ad.bind.dn",
    "index": 2,
    "serviceName": "RANGER"
  },
  {
    "category": "ADSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.ad.bind.password",
    "index": 3,
    "serviceName": "RANGER"
  },
  {
    "category": "ADSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.ad.referral",
    "serviceName": "RANGER"
  },
  {
    "category": "ADSettings",
    "filename": "ranger-admin-site.xml",
    "name": "ranger.ldap.ad.user.searchfilter",
    "serviceName": "RANGER"
  }
];
