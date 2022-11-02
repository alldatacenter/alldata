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

def get_component_repository_version(service_name = None, component_name = None, default_value = None):
  """
  Gets the version associated with the specified component from the structure in the command.
  Every command should contain a mapping of service/component to the desired repository it's set
  to.

  :service_name: the name of the service
  :component_name: the name of the component
  :default_value: the value to return if either the service or the component are not found
  """
  config = Script.get_config()

  versions = _get_component_repositories(config)
  if versions is None:
    return default_value

  if service_name is None:
    service_name = config['serviceName'] if config is not None and 'serviceName' in config else None

  if service_name is None or service_name not in versions:
    return default_value

  component_versions = versions[service_name]
  if len(component_versions) == 0:
    return default_value

  if component_name is None:
    component_name = config["role"] if config is not None and "role" in config else None

  # return a direct match of component name
  if component_name is not None and component_name in component_versions:
    return component_versions[component_name]

  # fall back to the first one for the service
  return component_versions.values()[0]


def _get_component_repositories(config):
  """
  Gets an initialized dictionary from the value in componentVersionMap. This structure is
  sent on every command by Ambari and should contain each service & component's desired repository.
  :config:  the configuration dictionary
  :return:
  """
  if "componentVersionMap" not in config or config["componentVersionMap"] is "":
    return None

  return config["componentVersionMap"]
