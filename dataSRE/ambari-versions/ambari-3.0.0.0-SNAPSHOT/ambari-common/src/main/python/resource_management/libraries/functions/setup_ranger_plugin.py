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
__all__ = ["setup_ranger_plugin"]

import os
from datetime import datetime
from resource_management.libraries.functions.ranger_functions import Rangeradmin
from resource_management.core.resources import File, Execute
from resource_management.libraries.functions.format import format
from resource_management.libraries.functions.get_stack_version import get_stack_version
from resource_management.core.logger import Logger
from resource_management.core.source import DownloadSource
from resource_management.libraries.resources import ModifyPropertiesFile
from resource_management.core.exceptions import Fail
from resource_management.libraries.functions.ranger_functions_v2 import RangeradminV2
from resource_management.libraries.script.script import Script

def setup_ranger_plugin(component_select_name, service_name, previous_jdbc_jar,
                        downloaded_custom_connector, driver_curl_source,
                        driver_curl_target, java_home,
                        repo_name, plugin_repo_dict, 
                        ranger_env_properties, plugin_properties,
                        policy_user, policymgr_mgr_url,
                        plugin_enabled, component_user, component_group, api_version=None, skip_if_rangeradmin_down = True, **kwargs):

  if driver_curl_source and not driver_curl_source.endswith("/None"):
    if previous_jdbc_jar and os.path.isfile(previous_jdbc_jar):
      File(previous_jdbc_jar, action='delete')

    File(downloaded_custom_connector,
        content = DownloadSource(driver_curl_source),
        mode = 0644
    )

    Execute(('cp', '--remove-destination', downloaded_custom_connector, driver_curl_target),
            path=["/bin", "/usr/bin/"],
            sudo=True
    )

    File(driver_curl_target, mode=0644)

  if policymgr_mgr_url.endswith('/'):
    policymgr_mgr_url = policymgr_mgr_url.rstrip('/')
  stack_root = Script.get_stack_root()
  stack_version = get_stack_version(component_select_name)
  file_path = format('{stack_root}/{stack_version}/ranger-{service_name}-plugin/install.properties')
  
  if not os.path.isfile(file_path):
    raise Fail(format('Ranger {service_name} plugin install.properties file does not exist at {file_path}'))
  
  ModifyPropertiesFile(file_path,
    properties = plugin_properties
  )

  custom_plugin_properties = dict()
  custom_plugin_properties['CUSTOM_USER'] = component_user
  custom_plugin_properties['CUSTOM_GROUP'] = component_group
  ModifyPropertiesFile(file_path,properties = custom_plugin_properties)

  if plugin_enabled:
    cmd = (format('enable-{service_name}-plugin.sh'),)
    if api_version == 'v2' and api_version is not None:
      ranger_adm_obj = RangeradminV2(url=policymgr_mgr_url, skip_if_rangeradmin_down = skip_if_rangeradmin_down)
    else:
      ranger_adm_obj = Rangeradmin(url=policymgr_mgr_url, skip_if_rangeradmin_down = skip_if_rangeradmin_down)

    ranger_adm_obj.create_ranger_repository(service_name, repo_name, plugin_repo_dict,
                                            ranger_env_properties['ranger_admin_username'], ranger_env_properties['ranger_admin_password'], 
                                            ranger_env_properties['admin_username'], ranger_env_properties['admin_password'], 
                                            policy_user)
  else:
    cmd = (format('disable-{service_name}-plugin.sh'),)
    
  cmd_env = {'JAVA_HOME': java_home,
             'PWD': format('{stack_root}/{stack_version}/ranger-{service_name}-plugin'),
             'PATH': format('{stack_root}/{stack_version}/ranger-{service_name}-plugin')}
  
  Execute(cmd, 
        environment=cmd_env, 
        logoutput=True,
        sudo=True,
  )
