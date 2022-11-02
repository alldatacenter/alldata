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
    "category": "KAFKA_BROKER",
    "filename": "kafka-broker.xml",
    "index": 1,
    "name": "log.dirs",
    "serviceName": "KAFKA"
  },
  {
    "category": "KAFKA_BROKER",
    "filename": "kafka-broker.xml",
    "name": "listeners",
    "serviceName": "KAFKA"
  },
  {
    "category": "KAFKA_BROKER",
    "filename": "kafka-broker.xml",
    "index": 0,
    "name": "log.roll.hours",
    "serviceName": "KAFKA"
  },
  {
    "category": "KAFKA_BROKER",
    "filename": "kafka-broker.xml",
    "index": 0,
    "name": "log.retention.hours",
    "serviceName": "KAFKA"
  },
  {
    "category": "KAFKA_BROKER",
    "filename": "kafka-broker.xml",
    "index": 0,
    "name": "zookeeper.connect",
    "serviceName": "KAFKA"
  },
  {
    "filename": "kafka-env.xml",
    "index": 0,
    "name": "kafka_pid_dir",
    "serviceName": "KAFKA"
  },
  {
    "filename": "ranger-kafka-plugin-properties.xml",
    "index": 1,
    "name": "ranger-kafka-plugin-enabled",
    "serviceName": "KAFKA"
  }
];