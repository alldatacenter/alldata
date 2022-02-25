from __future__ import absolute_import

from coilmq.protocol import STOMP10

"""
Core STOMP server logic, abstracted from socket transport implementation.

While this is abstracted from the socket transport implementation, it does
operate on the assumption that there is an available connection to send response
frames.

We're also making some simplified assumptions here which may make this engine
impractical for [high-performance] use in specifically asynchronous frameworks.
More specifically, this class was not explicitly designed around async patterns, 
meaning that it would likely be problematic to use with a framework like Twisted 
if the underlying storage implementations were processor intensive (e.g. database
access).  For the default memory storage engines, this shouldn't be a problem.

This code is inspired by the design of the Ruby stompserver project, by 
Patrick Hurley and Lionel Bouton.  See http://stompserver.rubyforge.org/
"""
import logging
from collections import defaultdict

__authors__ = ['"Hans Lellelid" <hans@xmpl.org>']
__copyright__ = "Copyright 2009 Hans Lellelid"
__license__ = """Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
 
  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License."""


class StompEngine(object):
    """ 
    The engine provides the core business logic that we use to respond to STOMP protocol
    messages.  

    This class is transport-agnostic; it exposes methods that expect STOMP frames and
    uses the attached connection to send frames to connected clients.

    @ivar connection: The connection (aka "protocol") backing this engine.
    @type connection: L{coilmq.server.StompConnection}

    @ivar authenticator: An C{Authenticator} implementation to use.  Setting this value
                            will implicitly cause authentication to be required.
    @type authenticator: L{coilmq.auth.Authenticator}

    @ivar queue_manager: The C{QueueManager} implementation to use.
    @type queue_manager: L{coilmq.queue.QueueManager}

    @ivar topic_manager: The C{TopicManager} implementation to use.
    @type topic_manager: L{coilmq.topic.TopicManager}

    @ivar transactions: Active transactions for this connection.
    @type transactions: C{dict} of C{str} to C{list} 

    @ivar connected: Whether engine is connected.
    @type connected: C{bool}
    """

    def __init__(self, connection, authenticator, queue_manager, topic_manager, protocol=STOMP10):
        """
        @param connection: The stomp connection backing this engine.
        @type connection: L{coilmq.server.StompConnection}
        """
        self.log = logging.getLogger('%s.%s' % (
            self.__class__.__module__, self.__class__.__name__))
        self.connection = connection
        self.authenticator = authenticator
        self.queue_manager = queue_manager
        self.topic_manager = topic_manager
        self.connected = False
        self.transactions = defaultdict(list)

        self.protocol = protocol(self)

    def process_frame(self, frame):
        self.protocol.process_frame(frame)

    def unbind(self):
        """
        Unbinds this connection from queue and topic managers (freeing up resources)
        and resets state.
        """
        self.connected = False
        self.queue_manager.disconnect(self.connection)
        self.topic_manager.disconnect(self.connection)
