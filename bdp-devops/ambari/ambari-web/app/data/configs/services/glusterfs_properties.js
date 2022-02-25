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
    "filename": "core-site.xml",
    "name": "fs.glusterfs.impl",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General",
    "filename": "core-site.xml",
    "name": "fs.AbstractFileSystem.glusterfs.impl",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "index": 1,
    "name": "hadoop_heapsize",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "hdfs_log_dir_prefix",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "hadoop_pid_dir_prefix",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "namenode_heapsize",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "namenode_opt_newsize",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "namenode_opt_maxnewsize",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "namenode_opt_permsize",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "namenode_opt_maxpermsize",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "dtnode_heapsize",
    "serviceName": "GLUSTERFS"
  },
  {
    "category": "General Hadoop",
    "filename": "hadoop-env.xml",
    "name": "glusterfs_user",
    "serviceName": "GLUSTERFS"
  }
];