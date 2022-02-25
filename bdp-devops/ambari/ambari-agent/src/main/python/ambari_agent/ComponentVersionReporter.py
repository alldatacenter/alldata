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

import logging
import threading

from ambari_agent import Constants
from collections import defaultdict

from ambari_agent.models.commands import AgentCommand

logger = logging.getLogger(__name__)

class ComponentVersionReporter(threading.Thread):
  def __init__(self, initializer_module):
    self.initializer_module = initializer_module
    self.topology_cache = initializer_module.topology_cache
    self.customServiceOrchestrator = initializer_module.customServiceOrchestrator
    self.server_responses_listener = initializer_module.server_responses_listener
    threading.Thread.__init__(self)

  def run(self):
    """
    Get version of all components by running get_version execution command.
    """
    try:
      cluster_reports = defaultdict(lambda:[])

      for cluster_id in self.topology_cache.get_cluster_ids():
        topology_cache = self.topology_cache[cluster_id]

        if 'components' not in topology_cache:
          continue

        current_host_id = self.topology_cache.get_current_host_id(cluster_id)

        if current_host_id is None:
          continue

        cluster_components = topology_cache.components
        for component_dict in cluster_components:
          # check if component is installed on current host
          if current_host_id not in component_dict.hostIds:
            continue

          service_name = component_dict.serviceName
          component_name = component_dict.componentName

          result = self.check_component_version(cluster_id, service_name, component_name)

          if result:
            cluster_reports[cluster_id].append(result)

      self.send_updates_to_server(cluster_reports)
    except:
      logger.exception("Exception in ComponentVersionReporter")

  def check_component_version(self, cluster_id, service_name, component_name):
    """
    Returns components version
    """
    # if not a component
    if self.topology_cache.get_component_info_by_key(cluster_id, service_name, component_name) is None:
      return None

    command_dict = {
      'serviceName': service_name,
      'role': component_name,
      'clusterId': cluster_id,
      'commandType': AgentCommand.get_version,
    }

    version_result = self.customServiceOrchestrator.requestComponentStatus(command_dict, command_name=AgentCommand.get_version)

    if version_result['exitcode'] or not 'structuredOut' in version_result or not 'version' in version_result['structuredOut']:
      logger.error("Could not get version for component {0} of {1} service cluster_id={2}. Command returned: {3}".format(component_name, service_name, cluster_id, version_result))
      return None

    # TODO: check if no strout or version if not there

    result = {
      'serviceName': service_name,
      'componentName': component_name,
      'version': version_result['structuredOut']['version'],
      'clusterId': cluster_id,
    }

    return result

  def send_updates_to_server(self, cluster_reports):
    if not cluster_reports or not self.initializer_module.is_registered:
      return

    self.initializer_module.connection.send(message={'clusters': cluster_reports}, destination=Constants.COMPONENT_VERSION_REPORTS_ENDPOINT)
