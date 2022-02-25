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
    "category": "OOZIE_SERVER",
    "filename": "oozie-site.xml",
    "index": 4,
    "name": "oozie.db.schema.name",
    "serviceName": "OOZIE"
  },
  {
    "category": "OOZIE_SERVER",
    "filename": "oozie-site.xml",
    "index": 5,
    "name": "oozie.service.JPAService.jdbc.username",
    "serviceName": "OOZIE"
  },
  {
    "category": "OOZIE_SERVER",
    "filename": "oozie-site.xml",
    "index": 6,
    "name": "oozie.service.JPAService.jdbc.password",
    "serviceName": "OOZIE"
  },
  {
    "category": "OOZIE_SERVER",
    "filename": "oozie-site.xml",
    "index": 7,
    "name": "oozie.service.JPAService.jdbc.driver",
    "serviceName": "OOZIE"
  },
  {
    "category": "OOZIE_SERVER",
    "filename": "oozie-site.xml",
    "index": 8,
    "name": "oozie.service.JPAService.jdbc.url",
    "serviceName": "OOZIE"
  },
  {
    "category": "OOZIE_SERVER",
    "displayType": "radio button",
    "filename": "oozie-env.xml",
    "index": 2,
    "name": "oozie_database",
    "options": [
      {
        "displayName": "New Derby Database",
        "hidden": false
      },
      {
        "displayName": "Existing MySQL / MariaDB Database",
        "hidden": false
      },
      {
        "displayName": "Existing PostgreSQL Database",
        "hidden": false
      },
      {
        "displayName": "Existing Oracle Database",
        "hidden": false
      }
    ],
    "radioName": "oozie-database",
    "serviceName": "OOZIE"
  },
  {
    "category": "OOZIE_SERVER",
    "filename": "oozie-env.xml",
    "index": 9,
    "name": "oozie_data_dir",
    "serviceName": "OOZIE"
  }
];
