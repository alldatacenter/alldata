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

import threading
import logging

from ambari_agent.CommandHooksOrchestrator import HooksOrchestrator
from ambari_agent.FileCache import FileCache
from ambari_agent.AmbariConfig import AmbariConfig
from ambari_agent.ClusterConfigurationCache import ClusterConfigurationCache
from ambari_agent.ClusterTopologyCache import ClusterTopologyCache
from ambari_agent.ClusterMetadataCache import ClusterMetadataCache
from ambari_agent.ClusterHostLevelParamsCache import ClusterHostLevelParamsCache
from ambari_agent.ClusterAlertDefinitionsCache import ClusterAlertDefinitionsCache
from ambari_agent.ActionQueue import ActionQueue
from ambari_agent.CommandStatusDict import CommandStatusDict
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator
from ambari_agent.RecoveryManager import RecoveryManager
from ambari_agent.AlertSchedulerHandler import AlertSchedulerHandler
from ambari_agent.ConfigurationBuilder import ConfigurationBuilder
from ambari_agent.StaleAlertsMonitor import StaleAlertsMonitor
from ambari_stomp.adapter.websocket import ConnectionIsAlreadyClosed
from ambari_agent.listeners.ServerResponsesListener import ServerResponsesListener

from ambari_agent import HeartbeatThread
from ambari_agent.ComponentStatusExecutor import ComponentStatusExecutor
from ambari_agent.CommandStatusReporter import CommandStatusReporter
from ambari_agent.HostStatusReporter import HostStatusReporter
from ambari_agent.AlertStatusReporter import AlertStatusReporter

logger = logging.getLogger(__name__)


class InitializerModule:
  """
  - Instantiate some singleton classes or widely used instances along with providing their dependencies.
  - Reduce cross modules dependencies.
  - Make other components code cleaner.
  - Provide an easier way to mock some dependencies.
  """
  def __init__(self):
    self.stop_event = threading.Event()
    self.config = AmbariConfig.get_resolved_config()

    self.is_registered = None
    self.metadata_cache = None
    self.topology_cache = None
    self.host_level_params_cache = None
    self.configurations_cache = None
    self.alert_definitions_cache = None
    self.configuration_builder = None
    self.stale_alerts_monitor = None
    self.server_responses_listener = None
    self.file_cache = None
    self.customServiceOrchestrator = None
    self.hooks_orchestrator = None
    self.recovery_manager = None
    self.commandStatuses = None
    self.action_queue = None
    self.alert_scheduler_handler = None

    self.init()

  def init(self):
    """
    Initialize properties
    """
    self.is_registered = False

    self.metadata_cache = ClusterMetadataCache(self.config.cluster_cache_dir, self.config)
    self.topology_cache = ClusterTopologyCache(self.config.cluster_cache_dir, self.config)
    self.host_level_params_cache = ClusterHostLevelParamsCache(self.config.cluster_cache_dir)
    self.configurations_cache = ClusterConfigurationCache(self.config.cluster_cache_dir)
    self.alert_definitions_cache = ClusterAlertDefinitionsCache(self.config.cluster_cache_dir)
    self.configuration_builder = ConfigurationBuilder(self)
    self.stale_alerts_monitor = StaleAlertsMonitor(self)
    self.server_responses_listener = ServerResponsesListener(self)
    self.file_cache = FileCache(self.config)
    self.customServiceOrchestrator = CustomServiceOrchestrator(self)
    self.hooks_orchestrator = HooksOrchestrator(self)
    self.recovery_manager = RecoveryManager(self)
    self.commandStatuses = CommandStatusDict(self)

    self.init_threads()


  def init_threads(self):
    """
    Initialize thread objects
    """
    self.component_status_executor = ComponentStatusExecutor(self)
    self.action_queue = ActionQueue(self)
    self.alert_scheduler_handler = AlertSchedulerHandler(self)
    self.command_status_reporter = CommandStatusReporter(self)
    self.host_status_reporter = HostStatusReporter(self)
    self.alert_status_reporter = AlertStatusReporter(self)
    self.heartbeat_thread = HeartbeatThread.HeartbeatThread(self)

  @property
  def connection(self):
    try:
      return self._connection
    except AttributeError:
      """
      Can be a result of race condition:
      begin sending X -> got disconnected by HeartbeatThread -> continue sending X
      """
      raise ConnectionIsAlreadyClosed("Connection to server is not established")
