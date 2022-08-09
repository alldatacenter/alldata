"""
Non-durable topic support functionality.

This code is inspired by the design of the Ruby stompserver project, by 
Patrick Hurley and Lionel Bouton.  See http://stompserver.rubyforge.org/
"""
import logging
import threading
import uuid
from collections import defaultdict

from coilmq.util.concurrency import synchronized

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

lock = threading.RLock()


class TopicManager(object):
    """
    Class that manages distribution of messages to topic subscribers.

    This class uses C{threading.RLock} to guard the public methods.  This is probably
    a bit excessive, given 1) the actomic nature of basic C{dict} read/write operations 
    and  2) the fact that most of the internal data structures are keying off of the 
    STOMP connection, which is going to be thread-isolated.  That said, this seems like 
    the technically correct approach and should increase the chance of this code being
    portable to non-GIL systems.

    @ivar _topics: A dict of registered topics, keyed by destination.
    @type _topics: C{dict} of C{str} to C{set} of L{coilmq.server.StompConnection}
    """

    def __init__(self):
        self.log = logging.getLogger(
            '%s.%s' % (__name__, self.__class__.__name__))

        # Lock var is required for L{synchornized} decorator.
        self._lock = threading.RLock()

        self._topics = defaultdict(set)

        # TODO: If we want durable topics, we'll need a store for topics.

    @synchronized(lock)
    def close(self):
        """
        Closes all resources associated with this topic manager.

        (Currently this is simply here for API conformity w/ L{coilmq.queue.QueueManager}.)
        """
        self.log.info("Shutting down topic manager.")  # pragma: no cover

    @synchronized(lock)
    def subscribe(self, connection, destination):
        """
        Subscribes a connection to the specified topic destination. 

        @param connection: The client connection to subscribe.
        @type connection: L{coilmq.server.StompConnection}

        @param destination: The topic destination (e.g. '/topic/foo')
        @type destination: C{str} 
        """
        self.log.debug("Subscribing %s to %s" % (connection, destination))
        self._topics[destination].add(connection)

    @synchronized(lock)
    def unsubscribe(self, connection, destination):
        """
        Unsubscribes a connection from the specified topic destination. 

        @param connection: The client connection to unsubscribe.
        @type connection: L{coilmq.server.StompConnection}

        @param destination: The topic destination (e.g. '/topic/foo')
        @type destination: C{str} 
        """
        self.log.debug("Unsubscribing %s from %s" % (connection, destination))
        if connection in self._topics[destination]:
            self._topics[destination].remove(connection)

        if not self._topics[destination]:
            del self._topics[destination]

    @synchronized(lock)
    def disconnect(self, connection):
        """
        Removes a subscriber connection.

        @param connection: The client connection to unsubscribe.
        @type connection: L{coilmq.server.StompConnection}
        """
        self.log.debug("Disconnecting %s" % connection)
        for dest in list(self._topics.keys()):
            if connection in self._topics[dest]:
                self._topics[dest].remove(connection)
            if not self._topics[dest]:
                # This won't trigger RuntimeError, since we're using keys()
                del self._topics[dest]

    @synchronized(lock)
    def send(self, message):
        """
        Sends a message to all subscribers of destination.

        @param message: The message frame.  (The frame will be modified to set command 
                            to MESSAGE and set a message id.)
        @type message: L{stompclient.frame.Frame}
        """
        dest = message.headers.get('destination')
        if not dest:
            raise ValueError(
                "Cannot send frame with no destination: %s" % message)

        message.cmd = 'message'

        message.headers.setdefault('message-id', str(uuid.uuid4()))

        bad_subscribers = set()
        for subscriber in self._topics[dest]:
            try:
                subscriber.send_frame(message)
            except:
                self.log.exception(
                    "Error delivering message to subscriber %s; client will be disconnected." % subscriber)
                # We queue for deletion so we are not modifying the topics dict
                # while iterating over it.
                bad_subscribers.add(subscriber)

        for subscriber in bad_subscribers:
            self.disconnect(subscriber)
