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

import logging
import threading
from socket import error as socket_error

from ambari_agent import Constants
from ambari_agent.Register import Register
from ambari_agent.Utils import BlockingDictionary
from ambari_agent.Utils import Utils
from ambari_agent.ComponentVersionReporter import ComponentVersionReporter
from ambari_agent.listeners.TopologyEventListener import TopologyEventListener
from ambari_agent.listeners.ConfigurationEventListener import ConfigurationEventListener
from ambari_agent.listeners.AgentActionsListener import AgentActionsListener
from ambari_agent.listeners.MetadataEventListener import MetadataEventListener
from ambari_agent.listeners.CommandsEventListener import CommandsEventListener
from ambari_agent.listeners.HostLevelParamsEventListener import HostLevelParamsEventListener
from ambari_agent.listeners.AlertDefinitionsEventListener import AlertDefinitionsEventListener
from ambari_agent.listeners.EncryptionKeyListener import EncryptionKeyListener
from ambari_agent import security
from ambari_stomp.adapter.websocket import ConnectionIsAlreadyClosed

HEARTBEAT_INTERVAL = 10
REQUEST_RESPONSE_TIMEOUT = 10

logger = logging.getLogger(__name__)

class HeartbeatThread(threading.Thread):
  """
  This thread handles registration and heartbeating routine.
  """
  def __init__(self, initializer_module):
    threading.Thread.__init__(self)
    self.heartbeat_interval = HEARTBEAT_INTERVAL
    self.stop_event = initializer_module.stop_event

    self.registration_builder = Register(initializer_module.config)

    self.initializer_module = initializer_module
    self.config = initializer_module.config

    # listeners
    self.server_responses_listener = initializer_module.server_responses_listener
    self.commands_events_listener = CommandsEventListener(initializer_module)
    self.metadata_events_listener = MetadataEventListener(initializer_module)
    self.topology_events_listener = TopologyEventListener(initializer_module)
    self.configuration_events_listener = ConfigurationEventListener(initializer_module)
    self.encryption_key_events_listener = EncryptionKeyListener(initializer_module)
    self.host_level_params_events_listener = HostLevelParamsEventListener(initializer_module)
    self.alert_definitions_events_listener = AlertDefinitionsEventListener(initializer_module)
    self.agent_actions_events_listener = AgentActionsListener(initializer_module)
    self.component_status_executor = initializer_module.component_status_executor
    self.listeners = [self.server_responses_listener, self.commands_events_listener, self.metadata_events_listener, self.topology_events_listener, self.configuration_events_listener, self.host_level_params_events_listener, self.alert_definitions_events_listener, self.agent_actions_events_listener, self.encryption_key_events_listener]

    self.post_registration_requests = [
    (Constants.TOPOLOGY_REQUEST_ENDPOINT, initializer_module.topology_cache, self.topology_events_listener, Constants.TOPOLOGIES_TOPIC),
    (Constants.METADATA_REQUEST_ENDPOINT, initializer_module.metadata_cache, self.metadata_events_listener, Constants.METADATA_TOPIC),
    (Constants.CONFIGURATIONS_REQUEST_ENDPOINT, initializer_module.configurations_cache, self.configuration_events_listener, Constants.CONFIGURATIONS_TOPIC),
    (Constants.HOST_LEVEL_PARAMS_TOPIC_ENPOINT, initializer_module.host_level_params_cache, self.host_level_params_events_listener, Constants.HOST_LEVEL_PARAMS_TOPIC),
    (Constants.ALERTS_DEFINITIONS_REQUEST_ENDPOINT, initializer_module.alert_definitions_cache, self.alert_definitions_events_listener, Constants.ALERTS_DEFINITIONS_TOPIC)
    ]
    self.responseId = 0
    self.file_cache = initializer_module.file_cache
    self.stale_alerts_monitor = initializer_module.stale_alerts_monitor
    self.post_registration_actions = [self.file_cache.reset, initializer_module.component_status_executor.clean_not_existing_clusters_info,
                                      initializer_module.alert_status_reporter.clean_not_existing_clusters_info, initializer_module.host_status_reporter.clean_cache]



  def run(self):
    """
    Run an endless loop of hearbeat with registration upon init or exception in heartbeating.
    """
    while not self.stop_event.is_set():
      try:
        if not self.initializer_module.is_registered:
          self.register()

        heartbeat_body = self.get_heartbeat_body()
        logger.debug("Heartbeat body is {0}".format(heartbeat_body))
        response = self.blocking_request(heartbeat_body, Constants.HEARTBEAT_ENDPOINT)
        logger.debug("Heartbeat response is {0}".format(response))
        self.handle_heartbeat_reponse(response)
      except Exception as ex:
        if isinstance(ex, (ConnectionIsAlreadyClosed)):
          logger.info("Connection was closed. Re-running the registration")
        elif isinstance(ex, (socket_error)):
          logger.info("Connection error \"{0}\". Re-running the registration".format(str(ex)))
        else:
          logger.exception("Exception in HeartbeatThread. Re-running the registration")

        self.unregister()

      self.stop_event.wait(self.heartbeat_interval)

    self.unregister()
    logger.info("HeartbeatThread has successfully finished")

  def register(self):
    """
    Subscribe to topics, register with server, wait for server's response.
    """
    self.establish_connection()

    self.add_listeners()
    self.subscribe_to_topics(Constants.PRE_REGISTRATION_TOPICS_TO_SUBSCRIBE)

    registration_request = self.registration_builder.build()
    logger.info("Sending registration request")
    logger.debug("Registration request is {0}".format(registration_request))

    response = self.blocking_request(registration_request, Constants.REGISTRATION_ENDPOINT)

    logger.info("Registration response received")
    logger.debug("Registration response is {0}".format(response))

    self.handle_registration_response(response)

    for endpoint, cache, listener, subscribe_to in self.post_registration_requests:
      try:
        listener.enabled = False
        self.subscribe_to_topics([subscribe_to])
        response = self.blocking_request({'hash': cache.hash}, endpoint, log_handler=listener.get_log_message)

        try:
          listener.on_event({}, response)
        except:
          logger.exception("Exception while handing response to request at {0} {1}".format(endpoint, response))
          raise
      finally:
        with listener.event_queue_lock:
          listener.enabled = True
          # Process queued messages if any
          listener.dequeue_unprocessed_events()

    self.subscribe_to_topics(Constants.POST_REGISTRATION_TOPICS_TO_SUBSCRIBE)

    self.run_post_registration_actions()

    self.initializer_module.is_registered = True
    # now when registration is done we can expose connection to other threads.
    self.initializer_module._connection = self.connection

    self.report_components_initial_versions()
    self.force_component_status_update()

  def run_post_registration_actions(self):
    for post_registration_action in self.post_registration_actions:
      post_registration_action()

  def report_components_initial_versions(self):
    ComponentVersionReporter(self.initializer_module).start()

  def force_component_status_update(self):
    self.component_status_executor.force_send_component_statuses()

  def unregister(self):
    """
    Disconnect and remove connection object from initializer_module so other threads cannot use it
    """
    self.initializer_module.is_registered = False

    if hasattr(self, 'connection'):
      try:
        self.connection.disconnect()
      except:
        logger.exception("Exception during self.connection.disconnect()")

      if hasattr(self.initializer_module, '_connection'):
        delattr(self.initializer_module, '_connection')
      delattr(self, 'connection')

      # delete any responses, which were not handled (possibly came during disconnect, etc.)
      self.server_responses_listener.reset_responses()

  def handle_registration_response(self, response):
    # exitstatus is a code of error which was raised on server side.
    # exitstatus = 0 (OK - Default)
    # exitstatus = 1 (Registration failed because different version of agent and server)
    exitstatus = 0
    if 'exitstatus' in response.keys():
      exitstatus = int(response['exitstatus'])

    if exitstatus != 0:
      # log - message, which will be printed to agents log
      if 'log' in response.keys():
        error_message = "Registration failed due to: {0}".format(response['log'])
      else:
        error_message = "Registration failed"

      raise Exception(error_message)

    self.responseId = int(response['id'])

  def handle_heartbeat_reponse(self, response):
    serverId = int(response['id'])

    if serverId != self.responseId + 1:
      logger.error("Error in responseId sequence - restarting")
      Utils.restartAgent(self.stop_event)
    else:
      self.responseId = serverId

  def get_heartbeat_body(self):
    """
    Heartbeat body to be send to server

    Heartbeat should as lightweight as possible.
    It purposes only connectivity and health check (whether all threads are ran correctly aka. stale_alerts, stale_component_status).
    Putting anything in heartbeat poses a problem for big clusters (e.g. 5K nodes) as heartbeats are sent often.

    Please use other event threads to send information.
    """
    body = {'id':self.responseId}

    stale_alerts = self.stale_alerts_monitor.get_stale_alerts()
    if stale_alerts:
      body['staleAlerts'] = stale_alerts

    return body


  def establish_connection(self):
    """
    Create a stomp connection
    """
    connection_url = 'wss://{0}:{1}/agent/stomp/v1'.format(self.config.server_hostname, self.config.secured_url_port)
    connection_helper = security.VerifiedHTTPSConnection(self.config.server_hostname, connection_url, self.config)
    self.connection = connection_helper.connect()

  def add_listeners(self):
    """
    Subscribe to topics and set listener classes.
    """
    for listener in self.listeners:
      self.connection.add_listener(listener)

  def subscribe_to_topics(self, topics_list):
    for topic_name in topics_list:
      self.connection.subscribe(destination=topic_name, id='sub', ack='client-individual')

  def blocking_request(self, message, destination, log_handler=None, timeout=REQUEST_RESPONSE_TIMEOUT):
    """
    Send a request to server and waits for the response from it. The response it detected by the correspondence of correlation_id.
    """
    def presend_hook(correlation_id):
      if log_handler:
        self.server_responses_listener.logging_handlers[correlation_id] = log_handler
           
    try:
      correlation_id = self.connection.send(message=message, destination=destination, presend_hook=presend_hook)
    except ConnectionIsAlreadyClosed:
      # this happens when trying to connect to broken connection. Happens if ambari-server is restarted.
      logger.warn("Connection failed while trying to connect to {0}".format(destination))
      raise

    try:
      return self.server_responses_listener.responses.blocking_pop(correlation_id, timeout=timeout)
    except BlockingDictionary.DictionaryPopTimeout:
      raise Exception("{0} seconds timeout expired waiting for response from server at {1} to message from {2}".format(timeout, Constants.SERVER_RESPONSES_TOPIC, destination))
