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

import logging
from ActualConfigHandler import ActualConfigHandler


class LiveStatus:

  SERVICES = []
  CLIENT_COMPONENTS = []
  COMPONENTS = []

  LIVE_STATUS = "STARTED"
  DEAD_STATUS = "INSTALLED"

  def __init__(self, cluster, service, component, globalConfig, config, configTags):
    self.logger = logging.getLogger()
    self.cluster = cluster
    self.service = service
    self.component = component
    self.globalConfig = globalConfig
    self.configTags = configTags
    self.actualConfigHandler = ActualConfigHandler(config, configTags)

  def build(self, component_status):
    """
    :param component_status: component status to include into report
    :return: populated livestatus dict
    """

    livestatus = {"componentName": self.component,
                  "msg": "",
                  "status": component_status,
                  "clusterName": self.cluster,
                  "serviceName": self.service,
                  "stackVersion": ""  # TODO: populate ?
                  }

    active_config = self.actualConfigHandler.read_actual_component(
      self.component)
    if active_config is not None:
      livestatus['configurationTags'] = active_config

    self.logger.debug("The live status for component %s of service %s is %s", self.component, self.service, livestatus)
    return livestatus
