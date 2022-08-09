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
    "category": "General",
    "filename": "accumulo-env.xml",
    "name": "accumulo_instance_name",
    "serviceName": "ACCUMULO"
  },
  {
    "category": "General",
    "filename": "accumulo-env.xml",
    "name": "accumulo_root_password",
    "serviceName": "ACCUMULO"
  },
  {
    "category": "General",
    "filename": "accumulo-site.xml",
    "name": "trace.user",
    "serviceName": "ACCUMULO"
  },
  {
    "category": "General",
    "filename": "accumulo-env.xml",
    "name": "trace_password",
    "serviceName": "ACCUMULO"
  },
  {
    "category": "General",
    "filename": "accumulo-env.xml",
    "name": "instance_secret",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 0,
    "name": "instance.volumes",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 1,
    "name": "instance.zookeeper.host",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 2,
    "name": "instance.zookeeper.timeout",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 3,
    "name": "master.port.client",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 4,
    "name": "tserver.port.client",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 5,
    "name": "monitor.port.client",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 6,
    "name": "monitor.port.log4j",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 7,
    "name": "gc.port.client",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 8,
    "name": "trace.port.client",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 9,
    "name": "tserver.memory.maps.native.enabled",
    "serviceName": "ACCUMULO"
  },
  {
    "filename": "accumulo-site.xml",
    "index": 10,
    "name": "general.classpaths",
    "serviceName": "ACCUMULO"
  }
];