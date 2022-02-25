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
import os
import sys
import logging
import json
import time
from coilmq.util import frames
from coilmq.util.frames import Frame

from BaseStompServerTestCase import BaseStompServerTestCase

from ambari_agent import HeartbeatThread
from ambari_agent.InitializerModule import InitializerModule
from ambari_agent.ComponentStatusExecutor import ComponentStatusExecutor
from ambari_agent.CommandStatusReporter import CommandStatusReporter
from ambari_agent.HostStatusReporter import HostStatusReporter
from ambari_agent.CustomServiceOrchestrator import CustomServiceOrchestrator
from ambari_agent.Utils import Utils

from mock.mock import MagicMock, patch

@patch("socket.gethostbyname", new=MagicMock(return_value="192.168.64.101"))
@patch("ambari_agent.hostname.hostname", new=MagicMock(return_value="c6401.ambari.apache.org"))
class TestAgentStompResponses:#(BaseStompServerTestCase):
  def setUp(self):
    self.maxDiff = None
    self.initializer_module = None

    self.remove_files(['/tmp/cluster_cache/configurations.json', '/tmp/cluster_cache/metadata.json', '/tmp/cluster_cache/topology.json', '/tmp/host_level_params.json', '/tmp/cluster_cache/alert_definitions.json'])

    if not os.path.exists("/tmp/ambari-agent"):
      os.mkdir("/tmp/ambari-agent")

    with open("/tmp/ambari-agent/version", "w") as fp:
      fp.write("2.5.0.0")

    return super(TestAgentStompResponses, self).setUp()

  def tearDown(self):
    if self.initializer_module:
      self.initializer_module.stop_event.set()

    return super(TestAgentStompResponses, self).tearDown()

  @patch.object(CustomServiceOrchestrator, "runCommand")
  def test_mock_server_can_start(self, runCommand_mock):
    runCommand_mock.return_value = {'stdout':'...', 'stderr':'...', 'structuredOut' : '{}', 'exitcode':1}

    self.initializer_module = InitializerModule()
    self.initializer_module.init()

    heartbeat_thread = HeartbeatThread.HeartbeatThread(self.initializer_module)
    heartbeat_thread.start()

    action_queue = self.initializer_module.action_queue
    action_queue.start()
    self.initializer_module.alert_scheduler_handler.start()

    component_status_executor = ComponentStatusExecutor(self.initializer_module)
    component_status_executor.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    registration_frame = self.server.frames_queue.get()

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)


    # response to /initial_topology
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '1'}, body=self.get_json("topology_create.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '2'}, body=self.get_json("metadata_after_registration.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '3'}, body=self.get_json("configurations_update.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '4'}, body=self.get_json("host_level_params.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '5'}, body=self.get_json("alert_definitions.json"))
    self.server.topic_manager.send(f)

    initial_topology_request = self.server.frames_queue.get()
    initial_metadata_request = self.server.frames_queue.get()
    initial_configs_request = self.server.frames_queue.get()
    initial_host_level_params_request = self.server.frames_queue.get()
    initial_alert_definitions_request = self.server.frames_queue.get()

    while not self.initializer_module.is_registered:
      time.sleep(0.1)

    command_status_reporter = CommandStatusReporter(self.initializer_module)
    command_status_reporter.start()

    host_status_reporter = HostStatusReporter(self.initializer_module)
    host_status_reporter.start()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/commands'}, body=self.get_json("execution_commands.json"))
    self.server.topic_manager.send(f)

    commands_subscribe_frame = self.server.frames_queue.get()
    configurations_subscribe_frame = self.server.frames_queue.get()
    metadata_subscribe_frame = self.server.frames_queue.get()
    topologies_subscribe_frame = self.server.frames_queue.get()
    host_level_params_subscribe_frame = self.server.frames_queue.get()
    alert_definitions_subscribe_frame = self.server.frames_queue.get()
    heartbeat_frame = self.server.frames_queue.get()
    dn_install_in_progress_frame = json.loads(self.server.frames_queue.get().body)
    dn_install_failed_frame = json.loads(self.server.frames_queue.get().body)
    zk_install_in_progress_frame = json.loads(self.server.frames_queue.get().body)
    zk_install_failed_frame = json.loads(self.server.frames_queue.get().body)
    action_status_in_progress_frame = json.loads(self.server.frames_queue.get().body)
    action_status_failed_frame = json.loads(self.server.frames_queue.get().body)
    dn_recovery_in_progress_frame = json.loads(self.server.frames_queue.get().body)
    dn_recovery_failed_frame = json.loads(self.server.frames_queue.get().body)
    host_status_report = json.loads(self.server.frames_queue.get().body)

    self.initializer_module.stop_event.set()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '6'}, body=json.dumps({'id':'1'}))
    self.server.topic_manager.send(f)

    command_status_reporter.join()
    heartbeat_thread.join()
    component_status_executor.join()
    host_status_reporter.join()
    action_queue.join()

    self.assertTrue('mounts' in host_status_report)
    self.assertEquals(self.initializer_module.topology_cache['0']['hosts'][0]['hostName'], 'c6401.ambari.apache.org')
    self.assertEquals(self.initializer_module.metadata_cache['0']['status_commands_to_run'], ('STATUS',))
    self.assertEquals(self.initializer_module.configurations_cache['0']['configurations']['zoo.cfg']['clientPort'], '2181')
    self.assertEquals(dn_install_in_progress_frame['clusters']['0'][0]['roleCommand'], 'INSTALL')
    self.assertEquals(dn_install_in_progress_frame['clusters']['0'][0]['role'], 'DATANODE')
    self.assertEquals(dn_install_in_progress_frame['clusters']['0'][0]['status'], 'IN_PROGRESS')
    self.assertEquals(dn_install_failed_frame['clusters']['0'][0]['status'], 'FAILED')
    self.assertEquals(dn_recovery_in_progress_frame['clusters']['0'][0]['roleCommand'], 'INSTALL')
    self.assertEquals(dn_recovery_in_progress_frame['clusters']['0'][0]['role'], 'DATANODE')
    self.assertEquals(dn_recovery_in_progress_frame['clusters']['0'][0]['status'], 'IN_PROGRESS')

    #============================================================================================
    #============================================================================================


    self.initializer_module = InitializerModule()
    self.initializer_module.init()

    self.server.frames_queue.queue.clear()

    heartbeat_thread = HeartbeatThread.HeartbeatThread(self.initializer_module)
    heartbeat_thread.start()


    action_queue = self.initializer_module.action_queue
    action_queue.start()
    self.initializer_module.alert_scheduler_handler.start()

    component_status_executor = ComponentStatusExecutor(self.initializer_module)
    component_status_executor.start()

    command_status_reporter = CommandStatusReporter(self.initializer_module)
    command_status_reporter.start()

    host_status_reporter = HostStatusReporter(self.initializer_module)
    host_status_reporter.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    registration_frame = self.server.frames_queue.get()

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '1'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '2'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '3'}, body='{"timestamp":1510577217}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '4'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '5'}, body='{}')
    self.server.topic_manager.send(f)

    commands_subscribe_frame = self.server.frames_queue.get()
    configurations_subscribe_frame = self.server.frames_queue.get()
    metadata_subscribe_frame = self.server.frames_queue.get()
    topologies_subscribe_frame = self.server.frames_queue.get()
    heartbeat_frame = self.server.frames_queue.get()
    status_reports_frame = self.server.frames_queue.get()

    self.initializer_module.stop_event.set()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '6'}, body=json.dumps({'id':'1'}))
    self.server.topic_manager.send(f)

    heartbeat_thread.join()
    component_status_executor.join()
    command_status_reporter.join()
    host_status_reporter.join()
    action_queue.join()


  def test_topology_update_and_delete(self):
    self.initializer_module = InitializerModule()
    self.initializer_module.init()

    heartbeat_thread = HeartbeatThread.HeartbeatThread(self.initializer_module)
    heartbeat_thread.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    registration_frame = self.server.frames_queue.get()

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)


    # response to /initial_topology
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '1'}, body=self.get_json("topology_create.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '2'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '3'}, body='{"timestamp":1510577217}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '4'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '5'}, body='{}')
    self.server.topic_manager.send(f)

    initial_topology_request = self.server.frames_queue.get()
    initial_metadata_request = self.server.frames_queue.get()
    initial_configs_request = self.server.frames_queue.get()
    initial_host_level_params_request = self.server.frames_queue.get()
    initial_alert_definitions_request = self.server.frames_queue.get()

    while not self.initializer_module.is_registered:
      time.sleep(0.1)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/topologies'}, body=self.get_json("topology_add_component.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/topologies'}, body=self.get_json("topology_add_component_host.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/topologies'}, body=self.get_json("topology_add_host.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/topologies'}, body=self.get_json("topology_delete_host.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/topologies'}, body=self.get_json("topology_delete_component.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/topologies'}, body=self.get_json("topology_delete_component_host.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/events/topologies'}, body=self.get_json("topology_delete_cluster.json"))
    self.server.topic_manager.send(f)

    def is_json_equal():
      #json_topology = json.dumps(self.initializer_module.topology_cache, indent=2, sort_keys=True)
      #json_excepted_lopology = json.dumps(self.get_dict_from_file("topology_cache_expected.json"), indent=2, sort_keys=True)
      #print json_topology
      #print json_excepted_lopology
      self.assertDictEqual(Utils.get_mutable_copy(self.initializer_module.topology_cache), self.get_dict_from_file("topology_cache_expected.json"))

    self.assert_with_retries(is_json_equal, tries=160, try_sleep=0.1)

    self.initializer_module.stop_event.set()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '6'}, body=json.dumps({'id':'1'}))
    self.server.topic_manager.send(f)

    heartbeat_thread.join()


  def test_alert_definitions_update_and_delete(self):
    self.initializer_module = InitializerModule()
    self.initializer_module.init()

    heartbeat_thread = HeartbeatThread.HeartbeatThread(self.initializer_module)
    heartbeat_thread.start()

    connect_frame = self.server.frames_queue.get()
    users_subscribe_frame = self.server.frames_queue.get()
    registration_frame = self.server.frames_queue.get()

    # server sends registration response
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '0'}, body=self.get_json("registration_response.json"))
    self.server.topic_manager.send(f)


    # response to /initial_topology
    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '1'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '2'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '3'}, body='{"timestamp":1510577217}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '4'}, body='{}')
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '5'}, body=self.get_json("alert_definitions_small.json"))
    self.server.topic_manager.send(f)

    initial_topology_request = self.server.frames_queue.get()
    initial_metadata_request = self.server.frames_queue.get()
    initial_configs_request = self.server.frames_queue.get()
    initial_host_level_params_request = self.server.frames_queue.get()
    initial_alert_definitions_request = self.server.frames_queue.get()

    while not self.initializer_module.is_registered:
      time.sleep(0.1)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/alert_definitions'}, body=self.get_json("alert_definitions_add.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/alert_definitions'}, body=self.get_json("alert_definitions_edit.json"))
    self.server.topic_manager.send(f)

    f = Frame(frames.MESSAGE, headers={'destination': '/user/alert_definitions'}, body=self.get_json("alert_definitions_delete.json"))
    self.server.topic_manager.send(f)


    def is_json_equal():
      #json_alert_definitions = json.dumps(self.initializer_module.alert_definitions_cache, indent=2, sort_keys=True)
      #json_excepted_definitions = json.dumps(self.get_dict_from_file("alert_definition_expected.json"), indent=2, sort_keys=True)
      #print json_definitions
      #print json_excepted_definitions
      self.assertDictEqual(Utils.get_mutable_copy(self.initializer_module.alert_definitions_cache), self.get_dict_from_file("alert_definition_expected.json"))

    self.assert_with_retries(is_json_equal, tries=160, try_sleep=0.1)

    self.initializer_module.stop_event.set()

    f = Frame(frames.MESSAGE, headers={'destination': '/user/', 'correlationId': '6'}, body=json.dumps({'id':'1'}))
    self.server.topic_manager.send(f)

    heartbeat_thread.join()

