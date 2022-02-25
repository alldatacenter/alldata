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
from ambari_agent import Utils
from ambari_agent import Constants

logger = logging.getLogger(__name__)

class ServerResponsesListener(EventListener):
  """
  Listener of Constants.SERVER_RESPONSES_TOPIC events from server.
  """
  RESPONSE_STATUS_STRING = 'status'
  RESPONSE_STATUS_SUCCESS = 'OK'

  def __init__(self, initializer_module):
    super(ServerResponsesListener, self).__init__(initializer_module)
    self.reset_responses()

  def on_event(self, headers, message):
    """
    Is triggered when an event to Constants.SERVER_RESPONSES_TOPIC topic is received from server.
    This type of event is general response to the agent request and contains 'correlationId', which is an int value
    of the request it responds to.

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    if Constants.CORRELATION_ID_STRING in headers:
      correlation_id = int(headers[Constants.CORRELATION_ID_STRING])
      self.responses.put(correlation_id, message)

      if correlation_id in self.listener_functions:
        self.listener_functions[correlation_id](headers, message)
        del self.listener_functions[correlation_id]

      if self.RESPONSE_STATUS_STRING in message and message[self.RESPONSE_STATUS_STRING] == self.RESPONSE_STATUS_SUCCESS:
        if correlation_id in self.listener_functions_on_success:
          self.listener_functions_on_success[correlation_id](headers, message)
          del self.listener_functions_on_success[correlation_id]
      else:
        if correlation_id in self.listener_functions_on_error:
          self.listener_functions_on_error[correlation_id](headers, message)
          del self.listener_functions_on_error[correlation_id]
    else:
      logger.warn("Received a message from server without a '{0}' header. Ignoring the message".format(Constants.CORRELATION_ID_STRING))

  def get_handled_path(self):
    return Constants.SERVER_RESPONSES_TOPIC

  def get_log_message(self, headers, message_json):
    """
    This string will be used to log received messsage of this type
    """
    if Constants.CORRELATION_ID_STRING in headers:
      correlation_id = int(headers[Constants.CORRELATION_ID_STRING])
      
      if correlation_id in self.logging_handlers:
        message_json = self.logging_handlers[correlation_id](headers, message_json)
        if message_json.startswith(" :"):
          message_json = message_json[2:]
        del self.logging_handlers[correlation_id]
      
      return " (correlation_id={0}): {1}".format(correlation_id, message_json)
    return str(message_json)

  def reset_responses(self):
    """
    Resets data saved on per-response basis.
    Should be called when correlactionIds are reset to 0 aka. re-registration case.
    """
    self.responses = Utils.BlockingDictionary()
    self.listener_functions_on_success = {}
    self.listener_functions_on_error = {}
    self.listener_functions = {}
    self.logging_handlers = {}


