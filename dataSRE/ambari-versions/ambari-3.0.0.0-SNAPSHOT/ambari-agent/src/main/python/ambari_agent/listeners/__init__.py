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

import ambari_simplejson as json
import ambari_stomp
import logging
import traceback
import copy
from ambari_stomp.adapter.websocket import ConnectionIsAlreadyClosed
from ambari_agent import Constants
from ambari_agent.Utils import Utils
from Queue import Queue
import threading

logger = logging.getLogger(__name__)

class EventListener(ambari_stomp.ConnectionListener):

  unprocessed_messages_queue = Queue(100)

  """
  Base abstract class for event listeners on specific topics.
  """
  def __init__(self, initializer_module):
    self.initializer_module = initializer_module
    self.enabled = True
    self.event_queue_lock = threading.RLock()

  def dequeue_unprocessed_events(self):
    while not self.unprocessed_messages_queue.empty():
      payload = self.unprocessed_messages_queue.get_nowait()
      if payload:
        logger.info("Processing event from unprocessed queue {0} {1}".format(payload[0], payload[1]))
        destination = payload[0]
        headers = payload[1]
        message_json = payload[2]
        message = payload[3]
        try:
          self.on_event(headers, message_json)
        except Exception as ex:
          logger.exception("Exception while handing event from {0} {1} {2}".format(destination, headers, message))
          self.report_status_to_sender(headers, message, ex)
        else:
          self.report_status_to_sender(headers, message)


  def on_message(self, headers, message):
    """
    This method is triggered by stomp when message from serve is received.

    Here we handle some decode the message to json and check if it addressed to this specific event listener.
    """
    if not 'destination' in headers:
      logger.warn("Received event from server which does not contain 'destination' header")
      return

    destination = headers['destination']
    if destination.rstrip('/') == self.get_handled_path().rstrip('/'):
      try:
        message_json = json.loads(message)
      except ValueError as ex:
        logger.exception("Received from server event is not a valid message json. Message is:\n{0}".format(message))
        self.report_status_to_sender(headers, message, ex)
        return

      if destination != Constants.ENCRYPTION_KEY_TOPIC:
        logger.info("Event from server at {0}{1}".format(destination, self.get_log_message(headers, copy.deepcopy(message_json))))

      if not self.enabled:
        with self.event_queue_lock:
          if not self.enabled:
            logger.info("Queuing event as unprocessed {0} since event "
                        "listener is disabled".format(destination))
            try:
              self.unprocessed_messages_queue.put_nowait((destination, headers, message_json, message))
            except Exception as ex:
              logger.warning("Cannot queue any more unprocessed events since "
                           "queue is full! {0} {1}".format(destination, message))
            return

      try:
        self.on_event(headers, message_json)
      except Exception as ex:
        logger.exception("Exception while handing event from {0} {1} {2}".format(destination, headers, message))
        self.report_status_to_sender(headers, message, ex)
      else:
        self.report_status_to_sender(headers, message)

  def report_status_to_sender(self, headers, message, ex=None):
    """
    Reports the status of delivery of the message to a sender

    @param headers: headers dictionary
    @param message: message payload dictionary
    @params ex: optional exception object for errors
    """
    if not Constants.MESSAGE_ID in headers:
      return

    if ex:
      confirmation_of_received = {Constants.MESSAGE_ID:headers[Constants.MESSAGE_ID], 'status':'ERROR', 'reason':Utils.get_traceback_as_text(ex)}
    else:
      confirmation_of_received = {Constants.MESSAGE_ID:headers[Constants.MESSAGE_ID], 'status':'OK'}

    try:
      connection = self.initializer_module.connection
    except ConnectionIsAlreadyClosed:
      # access early copy of connection before it is exposed globally
      connection = self.initializer_module.heartbeat_thread.connection

    try:
      connection.send(message=confirmation_of_received, destination=Constants.AGENT_RESPONSES_TOPIC)
    except:
      logger.exception("Could not send a confirmation '{0}' to server".format(confirmation_of_received))

  def on_event(self, headers, message):
    """
    Is triggered when an event for specific listener is received:

    @param headers: headers dictionary
    @param message: message payload dictionary
    """
    raise NotImplementedError()

  def get_log_message(self, headers, message_json):
    """
    This string will be used to log received messsage of this type
    """
    return ": " + str(message_json)