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

METADATA_DICTIONARY_KEY = 'metadataClusters'

class MetadataEventListener(EventListener):
  """
  Listener of Constants.METADATA_TOPIC events from server.
  """
  def __init__(self, initializer_module):
    super(MetadataEventListener, self).__init__(initializer_module)
    self.metadata_cache = initializer_module.metadata_cache

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.METADATA_TOPIC topic is received from server.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    # this kind of response is received if hash was identical. And server does not need to change anything
    if message == {}:
      return

    event_type = message['eventType']

    if event_type == 'CREATE':
      self.metadata_cache.rewrite_cache(message['clusters'], message['hash'])
    elif event_type == 'UPDATE':
      self.metadata_cache.cache_update(message['clusters'], message['hash'])
    elif event_type == 'DELETE':
      self.metadata_cache.cache_delete(message['clusters'], message['hash'])
    else:
      logger.error("Unknown event type '{0}' for metadata event")

  def get_handled_path(self):
    return Constants.METADATA_TOPIC