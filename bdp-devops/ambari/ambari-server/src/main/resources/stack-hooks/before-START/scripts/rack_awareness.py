#!/usr/bin/env python

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
"""

from resource_management.core.resources import File
from resource_management.core.source import StaticFile, Template
from resource_management.libraries.functions import format


def create_topology_mapping():
  import params

  File(params.net_topology_mapping_data_file_path,
       content=Template("topology_mappings.data.j2"),
       owner=params.hdfs_user,
       group=params.user_group,
       mode=0644,
       only_if=format("test -d {net_topology_script_dir}"))

def create_topology_script():
  import params

  File(params.net_topology_script_file_path,
       content=StaticFile('topology_script.py'),
       mode=0755,
       only_if=format("test -d {net_topology_script_dir}"))

def create_topology_script_and_mapping():
  import params
  if params.has_hadoop_env:
    create_topology_mapping()
    create_topology_script()
