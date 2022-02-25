# !/usr/bin/env python
"""
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

Ambari Agent

"""

_all__ = ["HdfsResource"]
from resource_management.core.base import Resource, ForcedListArgument, ResourceArgument, BooleanArgument

"""
Calling a lot of hadoop commands takes too much time.
The cause is that for every call new connection initialized, with datanodes, namenode.

While this resource can gather the directories/files to create/delete/copyFromLocal.
And after just with one call create all that.

action = create_on_execute / delete_on_execute / download_on_execute. Are for gathering information  about what you want
to create.

After everything is gathered you should execute action = execute. To perform delayed actions

The resource is a replacement for the following operations:
  1) hadoop fs -rmr
  2) hadoop fs -copyFromLocal
  3) hadoop fs -put
  4) hadoop fs -mkdir
  5) hadoop fs -touchz
  6) hadoop fs -chmod
  7) hadoop fs -chown
  8) hadoop fs -copyToLocal
"""


class HdfsResource(Resource):
  # Required: {target, type, action}
  # path to hadoop file/directory
  target = ResourceArgument(default=lambda obj: obj.name)
  # "directory" or "file"
  type = ResourceArgument()
  # "create_on_execute" or "delete_on_execute" or "download_on_execute" or "execute"
  action = ForcedListArgument()
  # if present - copies file/directory from local path {source} to hadoop path - {target}
  source = ResourceArgument()
  owner = ResourceArgument()
  group = ResourceArgument()
  mode = ResourceArgument()
  logoutput = ResourceArgument()
  recursive_chown = BooleanArgument(default=False)
  recursive_chmod = BooleanArgument(default=False)
  change_permissions_for_parents = BooleanArgument(default=False)
  """
  If a file present in HDFS is different from source, should it be replaced?
  """
  replace_existing_files = BooleanArgument(default=True)

  security_enabled = BooleanArgument(default=False)
  principal_name = ResourceArgument()
  keytab = ResourceArgument()
  kinit_path_local = ResourceArgument()
  user = ResourceArgument()
  hadoop_bin_dir = ResourceArgument()
  hadoop_conf_dir = ResourceArgument()
  
  """
  Path to file which contains '\n'-separated list of hdfs resources, which should not
  be managed. (simply any action to be skipped on it)
  
  This mean that if HdfsResource('/test1'..) is executed and /test1 is one of the lines
  in the given file, the execution will be ignored.
  
  Example value:
  /var/lib/ambari-agent/data/.hdfs_resource_ignore
  """
  hdfs_resource_ignore_file = ResourceArgument()

  """
  If the name of the HdfsResource is in immutable_paths
  and it is already created, any actions on it will be skipped
  (like changing permissions/recursive permissions, copying from source, deleting etc.)
  """
  immutable_paths = ResourceArgument(default=[])

  # WebHDFS needs these
  hdfs_site = ResourceArgument()
  default_fs = ResourceArgument()

  # To support HCFS
  dfs_type = ResourceArgument(default="")

  # default None - means all nameservices
  nameservices = ResourceArgument()

  #action 'execute' immediately performs all pending actions in an efficient manner
  #action 'create_on_execute/delete_on_execute/download_on_execute' adds to the list of pending actions
  actions = Resource.actions + ["create_on_execute", "delete_on_execute", "download_on_execute", "execute"]
