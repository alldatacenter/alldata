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
    "name": "admin_principal",
    "displayName": "Admin principal",
    "description": "Admin principal used to create principals and export key tabs (e.g. admin/admin@EXAMPLE.COM).",
    "isRequiredByAgent": false,
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Kadmin",
    "index": 1
  },
  {
    "name": "admin_password",
    "displayName": "Admin password",
    "displayType": "password",
    "isRequiredByAgent": false,
    "serviceName": "KERBEROS",
    "filename": "krb5-conf.xml",
    "category": "Kadmin",
    "index": 2
  },
  {
    "name": "dfs.ha.fencing.methods",
    "displayName": "dfs.ha.fencing.methods",
    "serviceName": "HDFS",
    "filename": "hdfs-site.xml",
    "category": "Advanced hdfs-site",
    "displayType": "multiLine",
    "index": 1
  }
];
