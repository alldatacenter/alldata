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
from resource_management.libraries.script.script import Script
from resource_management.libraries import functions
from resource_management.libraries.functions import format
from resource_management.libraries.functions.default import default
from ambari_commons import OSCheck

config = Script.get_config()
tmp_dir = Script.get_tmp_dir()

if OSCheck.is_windows_family():
  resourcemanager_win_service_name = 'resourcemanager'
  nodemanager_win_service_name = 'nodemanager'
  historyserver_win_service_name = 'historyserver'
  timelineserver_win_service_name = 'timelineserver'

  service_map = {
    'resourcemanager' : resourcemanager_win_service_name,
    'nodemanager' : nodemanager_win_service_name,
    'historyserver' : historyserver_win_service_name,
    'timelineserver' : timelineserver_win_service_name
  }
else:
  mapred_user = config['configurations']['mapred-env']['mapred_user']
  yarn_user = config['configurations']['yarn-env']['yarn_user']
  yarn_pid_dir_prefix = config['configurations']['yarn-env']['yarn_pid_dir_prefix']
  mapred_pid_dir_prefix = config['configurations']['mapred-env']['mapred_pid_dir_prefix']
  yarn_pid_dir = format("{yarn_pid_dir_prefix}/{yarn_user}")
  mapred_pid_dir = format("{mapred_pid_dir_prefix}/{mapred_user}")

  resourcemanager_pid_file = format("{yarn_pid_dir}/yarn-{yarn_user}-resourcemanager.pid")
  nodemanager_pid_file = format("{yarn_pid_dir}/yarn-{yarn_user}-nodemanager.pid")
  yarn_historyserver_pid_file_old = format("{yarn_pid_dir}/yarn-{yarn_user}-historyserver.pid")
  yarn_historyserver_pid_file = format("{yarn_pid_dir}/yarn-{yarn_user}-timelineserver.pid")  # *-historyserver.pid is deprecated
  mapred_historyserver_pid_file = format("{mapred_pid_dir}/mapred-{mapred_user}-historyserver.pid")

  hadoop_conf_dir = functions.conf_select.get_hadoop_conf_dir()

  hostname = config['hostname']
  kinit_path_local = functions.get_kinit_path(default('/configurations/kerberos-env/executable_search_paths', None))
  security_enabled = config['configurations']['cluster-env']['security_enabled']

stack_name = default("/hostLevelParams/stack_name", None)