"""
Classes that provide delivery scheduler implementations.

The default implementation used by the system for determining which subscriber
(connection) should receive a message is simply a random choice but favoring
reliable subscribers.  Developers can write their own delivery schedulers, which 
should implement the methods defined in L{QueuePriorityScheduler} if they would
like to customize the behavior.
"""
import abc
import random

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


class SubscriberPriorityScheduler(object):
    """ Abstract base class for choosing which recipient (subscriber) should receive a message. """
    __metaclass__ = abc.ABCMeta

    @abc.abstractmethod
    def choice(self, subscribers, message):
        """
        Chooses which subscriber (from list) should receive specified message.

        @param subscribers: Collection of subscribed connections eligible to receive message. 
        @type subscribers: C{list} of L{coilmq.server.StompConnection}

        @param message: The message to be delivered. 
        @type message: L{stompclient.frame.Frame}

        @return: A selected subscriber from the list or None if no subscriber could be chosen (e.g. list is empty).
        @rtype: L{coilmq.server.StompConnection}
        """


class QueuePriorityScheduler(object):
    """
    Abstract base class for objects that provide a way to prioritize the queues.
    """

    def choice(self, queues, connection):
        """
        Choose which queue to select for messages to specified connection.

        @param queues: A C{dict} mapping queue name to queues (sets of frames) to which 
                        specified connection is subscribed.
        @type queues:  C{dict} of C{str} to C{set} of L{stompclient.frame.Frame}

        @param connection: The connection that is going to be delivered the frame(s).
        @type connection: L{coilmq.server.StompConnection}

        @return: A selected queue destination (name) or None if queues C{dict} is empty.
        @rtype: C{str}
        """
        raise NotImplementedError


class RandomSubscriberScheduler(SubscriberPriorityScheduler):
    """ A delivery scheduler that chooses a random subscriber for message recipient. """

    def choice(self, subscribers, message):
        """
        Chooses a random connection from subscribers to deliver specified message.

        @param subscribers: Collection of subscribed connections to destination. 
        @type subscribers: C{list} of L{coilmq.server.StompConnection}

        @param message: The message to be delivered. 
        @type message: L{stompclient.frame.Frame}

        @return: A random subscriber from the list or None if list is empty.
        @rtype: L{coilmq.server.StompConnection}
        """
        if not subscribers:
            return None
        return random.choice(subscribers)


class FavorReliableSubscriberScheduler(SubscriberPriorityScheduler):
    """
    A random delivery scheduler which prefers reliable subscribers.
    """

    def choice(self, subscribers, message):
        """
        Choose a random connection, favoring those that are reliable from
        subscriber pool to deliver specified message.

        @param subscribers: Collection of subscribed connections to destination. 
        @type subscribers: C{list} of L{coilmq.server.StompConnection}

        @param message: The message to be delivered. 
        @type message: L{stompclient.frame.Frame}

        @return: A random subscriber from the list or None if list is empty.
        @rtype: L{coilmq.server.StompConnection}
        """
        if not subscribers:
            return None
        reliable_subscribers = [
            s for s in subscribers if s.reliable_subscriber]
        if reliable_subscribers:
            return random.choice(reliable_subscribers)
        else:
            return random.choice(subscribers)


class RandomQueueScheduler(QueuePriorityScheduler):
    """
    Implementation of L{QueuePriorityScheduler} that selects a random queue from the list.
    """

    def choice(self, queues, connection):
        """
        Chooses a random queue for messages to specified connection.

        @param queues: A C{dict} mapping queue name to queues (sets of frames) to which 
                        specified connection is subscribed.
        @type queues:  C{dict} of C{str} to C{set} of L{stompclient.frame.Frame}

        @param connection: The connection that is going to be delivered the frame(s).
        @type connection: L{coilmq.server.StompConnection}

        @return: A random queue destination or None if list is empty.
        @rtype: C{str}
        """
        if not queues:
            return None
        return random.choice(list(queues.keys()))
