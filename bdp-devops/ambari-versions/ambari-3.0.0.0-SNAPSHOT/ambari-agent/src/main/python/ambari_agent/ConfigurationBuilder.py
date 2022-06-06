#!/usr/bin/env python

'''
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
'''

import hostname


class ConfigurationBuilder:
  def __init__(self, initializer_module):
    self.config = initializer_module.config
    self.metadata_cache = initializer_module.metadata_cache
    self.topology_cache = initializer_module.topology_cache
    self.host_level_params_cache = initializer_module.host_level_params_cache
    self.configurations_cache = initializer_module.configurations_cache

  def get_configuration(self, cluster_id, service_name, component_name, configurations_timestamp=None):
    if cluster_id:
      if configurations_timestamp and self.configurations_cache.timestamp < configurations_timestamp:
        raise Exception("Command requires configs with timestamp={0} but configs on agent have timestamp={1}".format(configurations_timestamp, self.configurations_cache.timestamp))

      metadata_cache = self.metadata_cache[cluster_id]
      configurations_cache = self.configurations_cache[cluster_id]
      host_level_params_cache = self.host_level_params_cache[cluster_id]

      command_dict = {
        'clusterLevelParams': metadata_cache.clusterLevelParams,
        'hostLevelParams': host_level_params_cache,
        'clusterHostInfo': self.topology_cache.get_cluster_host_info(cluster_id),
        'localComponents': self.topology_cache.get_cluster_local_components(cluster_id),
        'componentVersionMap': self.topology_cache.get_cluster_component_version_map(cluster_id),
        'agentLevelParams': {'hostname': self.topology_cache.get_current_host_info(cluster_id)['hostName']},
        'clusterName': metadata_cache.clusterLevelParams.cluster_name
      }

      if service_name is not None and service_name != 'null':
        command_dict['serviceLevelParams'] = metadata_cache.serviceLevelParams[service_name]

      component_dict = self.topology_cache.get_component_info_by_key(cluster_id, service_name, component_name)
      if component_dict is not None:
        command_dict.update({
          'componentLevelParams': component_dict.componentLevelParams,
          'commandParams': component_dict.commandParams
        })

      command_dict.update(configurations_cache)
    else:
      command_dict = {'agentLevelParams': {}}

    command_dict['ambariLevelParams'] = self.metadata_cache.get_cluster_indepedent_data().clusterLevelParams

    command_dict['agentLevelParams'].update({
      'public_hostname': self.public_fqdn,
      'agentCacheDir': self.config.get('agent', 'cache_dir'),
    })
    command_dict['agentLevelParams']["agentConfigParams"] = {
      "agent": {
        "parallel_execution": self.config.get_parallel_exec_option(),
        "use_system_proxy_settings": self.config.use_system_proxy_setting()
      }
    }
    return command_dict

  @property
  def public_fqdn(self):
    return hostname.public_hostname(self.config)
