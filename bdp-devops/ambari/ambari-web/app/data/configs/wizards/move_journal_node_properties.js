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

module.exports =
{
  "moveJNConfig": {
    serviceName: 'MISC',
    displayName: 'MISC',
    configCategories: [
      App.ServiceConfigCategory.create({ name: 'HDFS', displayName: 'HDFS'})
    ],
    sites: ['hdfs-site'],
    configs: [
      {
        "name": "dfs.namenode.shared.edits.dir",
        "displayName": "dfs.namenode.shared.edits.dir",
        "description": " The URI which identifies the group of JNs where the NameNodes will write/read edits.",
        "isReconfigurable": false,
        "recommendedValue": "qjournal://node1.example.com:8485;node2.example.com:8485;node3.example.com:8485/mycluster",
        "value": "qjournal://node1.example.com:8485;node2.example.com:8485;node3.example.com:8485/mycluster",
        "category": "HDFS",
        "filename": "hdfs-site",
        "serviceName": 'MISC'
      }
    ]
  }
};
