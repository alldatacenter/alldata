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
import ambari_stomp

from ambari_agent.listeners import EventListener
from ambari_agent.Utils import Utils
from ambari_agent import Constants

logger = logging.getLogger(__name__)

class AgentActionsListener(EventListener):
  """
  Listener of Constants.AGENT_ACTIONS_TOPIC events from server.
  """
  ACTION_NAME = 'actionName'
  RESTART_AGENT_ACTION = 'RESTART_AGENT'
  
  def __init__(self, initializer_module):
    super(AgentActionsListener, self).__init__(initializer_module)
    self.stop_event = initializer_module.stop_event

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.AGENT_ACTIONS_TOPIC topic is received from server.
    It contains some small actions which server can ask agent to do.

    For bigger actions containing a lot of info and special workflow and a new topic would be
    required. Small actions like restart_agent/clean_cache make sense to be in a general event

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    action_name = message[self.ACTION_NAME]

    if action_name == self.RESTART_AGENT_ACTION:
      self.restart_agent()
    else:
      logger.warn("Unknown action '{0}' requested by server. Ignoring it".format(action_name))

  def restart_agent(self):
    logger.warn("Restarting the agent by the request from server")
    Utils.restartAgent(self.stop_event)

  def get_handled_path(self):
    return Constants.AGENT_ACTIONS_TOPIC


