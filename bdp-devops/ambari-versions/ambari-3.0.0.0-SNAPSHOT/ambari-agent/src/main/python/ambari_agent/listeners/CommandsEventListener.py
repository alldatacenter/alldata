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
from ambari_agent import Constants

logger = logging.getLogger(__name__)

class CommandsEventListener(EventListener):
  """
  Listener of Constants.CONFIGURATIONS_TOPIC events from server.
  """
  def __init__(self, initializer_module):
    super(CommandsEventListener, self).__init__(initializer_module)
    self.action_queue = initializer_module.action_queue

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.COMMANDS_TOPIC topic is received from server.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    ""
    commands = []
    cancel_commands = []
    for cluster_id in message['clusters'].keys():
      cluster_dict = message['clusters'][cluster_id]

      if 'commands' in cluster_dict:
        commands += cluster_dict['commands']
      if 'cancelCommands' in cluster_dict:
        cancel_commands += cluster_dict['cancelCommands']

    for command in commands:
      command['requiredConfigTimestamp'] = message['requiredConfigTimestamp']

    with self.action_queue.lock:
      self.action_queue.cancel(cancel_commands)
      self.action_queue.put(commands)

  def get_handled_path(self):
    return Constants.COMMANDS_TOPIC
    
  def get_log_message(self, headers, message_json):
    """
    This string will be used to log received messsage of this type.
    Usually should be used if full dict is too big for logs and should shortened or made more readable
    """
    try:
      for cluster_id in message_json['clusters']:
        for command in message_json['clusters'][cluster_id]['commands']:
          if 'repositoryFile' in command:
            command['repositoryFile'] = '...'
          if 'commandParams' in command:
            command['commandParams'] = '...'
          if 'clusterHostInfo' in command:
            command['clusterHostInfo'] = '...'
          if 'componentVersionMap' in command:
            command['componentVersionMap'] = '...'
    except KeyError:
      pass
      
    return super(CommandsEventListener, self).get_log_message(headers, message_json)