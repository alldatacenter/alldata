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

class TopologyEventListener(EventListener):
  """
  Listener of Constants.TOPOLOGIES_TOPIC events from server.
  """
  def __init__(self, initializer_module):
    super(TopologyEventListener, self).__init__(initializer_module)
    self.topology_cache = initializer_module.topology_cache

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.TOPOLOGIES_TOPIC topic is received from server.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    # this kind of response is received if hash was identical. And server does not need to change anything
    if message == {}:
      return

    event_type = message['eventType']

    if event_type == 'CREATE':
      self.topology_cache.rewrite_cache(message['clusters'], message['hash'])
    elif event_type == 'UPDATE':
      self.topology_cache.cache_update(message['clusters'], message['hash'])
    elif event_type == 'DELETE':
      self.topology_cache.cache_delete(message['clusters'], message['hash'])
    else:
      logger.error("Unknown event type '{0}' for topology event")

  def get_handled_path(self):
    return Constants.TOPOLOGIES_TOPIC
    
  def get_log_message(self, headers, message_json):
    """
    This string will be used to log received messsage of this type.
    Usually should be used if full dict is too big for logs and should shortened or made more readable
    """
    try:
      for cluster_id in message_json['clusters']:
        for component_info in message_json['clusters'][cluster_id]['components']:
          if 'componentLevelParams' in component_info:
            component_info['componentLevelParams'] = '...'
          if 'commandParams' in component_info:
            component_info['commandParams'] = '...'
    except KeyError:
      pass
      
    return super(TopologyEventListener, self).get_log_message(headers, message_json)