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
    "category": "ZOOKEEPER_SERVER",
    "filename": "zoo.cfg.xml",
    "index": 1,
    "name": "dataDir",
    "serviceName": "ZOOKEEPER"
  },
  {
    "category": "ZOOKEEPER_SERVER",
    "filename": "zoo.cfg.xml",
    "index": 2,
    "name": "tickTime",
    "serviceName": "ZOOKEEPER"
  },
  {
    "category": "ZOOKEEPER_SERVER",
    "filename": "zoo.cfg.xml",
    "index": 3,
    "name": "initLimit",
    "serviceName": "ZOOKEEPER"
  },
  {
    "category": "ZOOKEEPER_SERVER",
    "filename": "zoo.cfg.xml",
    "index": 4,
    "name": "syncLimit",
    "serviceName": "ZOOKEEPER"
  },
  {
    "category": "ZOOKEEPER_SERVER",
    "filename": "zoo.cfg.xml",
    "index": 5,
    "name": "clientPort",
    "serviceName": "ZOOKEEPER"
  },
  {
    "filename": "zookeeper-env.xml",
    "index": 0,
    "name": "zk_log_dir",
    "serviceName": "ZOOKEEPER"
  },
  {
    "filename": "zookeeper-env.xml",
    "index": 1,
    "name": "zk_pid_dir",
    "serviceName": "ZOOKEEPER"
  }
];