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

class ConfigurationEventListener(EventListener):
  """
  Listener of Constants.CONFIGURATIONS_TOPIC events from server.
  """
  def __init__(self, initializer_module):
    super(ConfigurationEventListener, self).__init__(initializer_module)
    self.configurations_cache = initializer_module.configurations_cache
    self.recovery_manager = initializer_module.recovery_manager

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.CONFIGURATIONS_TOPIC topic is received from server.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    self.configurations_cache.timestamp = message.pop('timestamp')

    # this kind of response is received if hash was identical. And server does not need to change anything
    if message == {}:
      return

    self.configurations_cache.rewrite_cache(message['clusters'], message['hash'])

    if message['clusters']:
      # FIXME: Recovery manager does not support multiple cluster as of now.
      self.recovery_manager.cluster_id = message['clusters'].keys()[0]
      self.recovery_manager.on_config_update()

  def get_handled_path(self):
    return Constants.CONFIGURATIONS_TOPIC
    
  def get_log_message(self, headers, message_json):
    """
    This string will be used to log received messsage of this type.
    Usually should be used if full dict is too big for logs and should shortened shortened or made more readable
    """
    try:
      for cluster_id in message_json['clusters']:
        for config_type in message_json['clusters'][cluster_id]['configurations']:
          message_json['clusters'][cluster_id]['configurations'][config_type] = '...'
    except KeyError:
      pass
      
    return super(ConfigurationEventListener, self).get_log_message(headers, message_json)