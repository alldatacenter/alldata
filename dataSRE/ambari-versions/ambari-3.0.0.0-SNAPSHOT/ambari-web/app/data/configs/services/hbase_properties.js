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
    "filename": "hbase-site.xml",
    "index": 0,
    "name": "hbase.hstore.compactionThreshold",
    "serviceName": "HBASE"
  },
  {
    "category": "General",
    "filename": "hbase-site.xml",
    "index": 1,
    "name": "hfile.block.cache.size",
    "serviceName": "HBASE"
  },
  {
    "category": "General",
    "filename": "hbase-site.xml",
    "index": 2,
    "name": "hbase.hregion.max.filesize",
    "serviceName": "HBASE"
  },
  {
    "category": "HBASE_REGIONSERVER",
    "filename": "hbase-site.xml",
    "index": 2,
    "name": "hbase.regionserver.handler.count",
    "serviceName": "HBASE"
  },
  {
    "category": "HBASE_REGIONSERVER",
    "filename": "hbase-site.xml",
    "index": 3,
    "name": "hbase.hregion.majorcompaction",
    "serviceName": "HBASE"
  },
  {
    "category": "HBASE_REGIONSERVER",
    "filename": "hbase-site.xml",
    "index": 4,
    "name": "hbase.hregion.memstore.block.multiplier",
    "serviceName": "HBASE"
  },
  {
    "category": "HBASE_REGIONSERVER",
    "filename": "hbase-site.xml",
    "index": 5,
    "name": "hbase.hregion.memstore.flush.size",
    "serviceName": "HBASE"
  },
  {
    "category": "General",
    "filename": "hbase-site.xml",
    "index": 3,
    "name": "hbase.client.scanner.caching",
    "serviceName": "HBASE"
  },
  {
    "category": "General",
    "filename": "hbase-site.xml",
    "index": 4,
    "name": "zookeeper.session.timeout",
    "serviceName": "HBASE"
  },
  {
    "category": "General",
    "filename": "hbase-site.xml",
    "index": 5,
    "name": "hbase.client.keyvalue.maxsize",
    "serviceName": "HBASE"
  },
  {
    "category": "HBASE_MASTER",
    "filename": "hbase-env.xml",
    "index": 1,
    "name": "hbase_master_heapsize",
    "serviceName": "HBASE"
  },
  {
    "category": "HBASE_REGIONSERVER",
    "filename": "hbase-env.xml",
    "index": 1,
    "name": "hbase_regionserver_heapsize",
    "serviceName": "HBASE"
  },
  {
    "category": "HBASE_REGIONSERVER",
    "filename": "hbase-env.xml",
    "index": 6,
    "name": "hbase_regionserver_xmn_max",
    "serviceName": "HBASE"
  },
  {
    "category": "HBASE_REGIONSERVER",
    "filename": "hbase-env.xml",
    "index": 7,
    "name": "hbase_regionserver_xmn_ratio",
    "serviceName": "HBASE"
  },
  {
    "filename": "ranger-hbase-plugin-properties.xml",
    "index": 1,
    "name": "ranger-hbase-plugin-enabled",
    "serviceName": "HBASE"
  }
];