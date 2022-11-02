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
    "category": "SECONDARY_NAMENODE",
    "filename": "hdfs-site.xml",
    "index": 1,
    "name": "dfs.namenode.checkpoint.dir",
    "serviceName": "HDFS"
  },
  {
    "category": "General",
    "filename": "hdfs-site.xml",
    "index": 3,
    "name": "dfs.namenode.checkpoint.period",
    "serviceName": "HDFS"
  },
  {
    "category": "NAMENODE",
    "filename": "hdfs-site.xml",
    "index": 1,
    "name": "dfs.namenode.name.dir",
    "serviceName": "HDFS"
  },
  {
    "category": "General",
    "filename": "hdfs-site.xml",
    "index": 0,
    "name": "dfs.webhdfs.enabled",
    "serviceName": "HDFS"
  },
  {
    "category": "DATANODE",
    "filename": "hdfs-site.xml",
    "index": 3,
    "name": "dfs.datanode.failed.volumes.tolerated",
    "serviceName": "HDFS"
  },
  {
    "category": "DATANODE",
    "filename": "hdfs-site.xml",
    "index": 1,
    "name": "dfs.datanode.data.dir",
    "serviceName": "HDFS"
  },
  {
    "category": "DATANODE",
    "filename": "hdfs-site.xml",
    "name": "dfs.datanode.data.dir.perm",
    "serviceName": "HDFS"
  },
  {
    "category": "NFS_GATEWAY",
    "filename": "hdfs-site.xml",
    "index": 1,
    "name": "nfs.file.dump.dir",
    "serviceName": "HDFS"
  },
  {
    "category": "General",
    "filename": "hdfs-site.xml",
    "index": 2,
    "name": "dfs.namenode.accesstime.precision",
    "serviceName": "HDFS"
  },
  {
    "category": "NFS_GATEWAY",
    "filename": "hdfs-site.xml",
    "index": 3,
    "name": "nfs.exports.allowed.hosts",
    "serviceName": "HDFS"
  },
  {
    "category": "General",
    "filename": "hdfs-site.xml",
    "name": "dfs.replication",
    "serviceName": "HDFS"
  },
  {
    "category": "General",
    "filename": "hdfs-site.xml",
    "index": 2,
    "name": "dfs.datanode.du.reserved",
    "serviceName": "HDFS"
  },
  {
    "category": "NAMENODE",
    "filename": "hadoop-env.xml",
    "index": 2,
    "name": "namenode_heapsize",
    "serviceName": "HDFS"
  },
  {
    "category": "NAMENODE",
    "filename": "hadoop-env.xml",
    "index": 3,
    "name": "namenode_opt_newsize",
    "serviceName": "HDFS"
  },
  {
    "category": "NAMENODE",
    "filename": "hadoop-env.xml",
    "index": 5,
    "name": "namenode_opt_permsize",
    "serviceName": "HDFS"
  },
  {
    "category": "NAMENODE",
    "filename": "hadoop-env.xml",
    "index": 6,
    "name": "namenode_opt_maxpermsize",
    "serviceName": "HDFS"
  },
  {
    "category": "NAMENODE",
    "filename": "hadoop-env.xml",
    "index": 4,
    "name": "namenode_opt_maxnewsize",
    "serviceName": "HDFS"
  },
  {
    "category": "DATANODE",
    "filename": "hadoop-env.xml",
    "index": 2,
    "name": "dtnode_heapsize",
    "serviceName": "HDFS"
  },
  {
    "category": "NFS_GATEWAY",
    "filename": "hadoop-env.xml",
    "index": 1,
    "name": "nfsgateway_heapsize",
    "serviceName": "HDFS"
  },
  {
    "category": "General",
    "filename": "hadoop-env.xml",
    "index": 1,
    "name": "hadoop_heapsize",
    "serviceName": "HDFS"
  },
  {
    "filename": "ranger-hdfs-plugin-properties.xml",
    "index": 1,
    "name": "ranger-hdfs-plugin-enabled",
    "serviceName": "HDFS"
  }
];