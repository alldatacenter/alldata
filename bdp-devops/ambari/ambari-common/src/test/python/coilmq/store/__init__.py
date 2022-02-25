"""
Storage containers for durable queues and (planned) durable topics.
"""
import abc
import logging
import threading

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


class QueueStore(object):
    """
    Abstract base class for queue storage. 

    Extensions/implementations of this class must be thread-safe.

    @ivar log: A logger for this class.
    @type log: C{logging.Logger}
    """
    __metaclass__ = abc.ABCMeta

    def __init__(self):
        """
        A base constructor that sets up logging.

        If you extend this class, you should either call this method or at minimum make sure these values
        get set.
        """
        self.log = logging.getLogger('%s.%s' % (
            self.__module__, self.__class__.__name__))

    @abc.abstractmethod
    @synchronized(lock)
    def enqueue(self, destination, frame):
        """
        Store message (frame) for specified destinationination.

        @param destination: The destinationination queue name for this message (frame).
        @type destination: C{str}

        @param frame: The message (frame) to send to specified destinationination.
        @type frame: C{stompclient.frame.Frame}
        """

    @abc.abstractmethod
    @synchronized(lock)
    def dequeue(self, destination):
        """
        Removes and returns an item from the queue (or C{None} if no items in queue).

        @param destination: The queue name (destinationination).
        @type destination: C{str}

        @return: The first frame in the specified queue, or C{None} if there are none.
        @rtype: C{stompclient.frame.Frame} 
        """

    @synchronized(lock)
    def requeue(self, destination, frame):
        """
        Requeue a message (frame) for storing at specified destinationination.

        @param destination: The destinationination queue name for this message (frame).
        @type destination: C{str}

        @param frame: The message (frame) to send to specified destinationination.
        @type frame: C{stompclient.frame.Frame}
        """
        self.enqueue(destination, frame)

    @synchronized(lock)
    def size(self, destination):
        """
        Size of the queue for specified destination.

        @param destination: The queue destination (e.g. /queue/foo)
        @type destination: C{str}

        @return: The number of frames in specified queue.
        @rtype: C{int}
        """
        raise NotImplementedError()

    @synchronized(lock)
    def has_frames(self, destination):
        """
        Whether specified destination has any frames.

        Default implementation uses L{QueueStore.size} to determine if there
        are any frames in queue.  Subclasses may choose to optimize this.

        @param destination: The queue destination (e.g. /queue/foo)
        @type destination: C{str}

        @return: The number of frames in specified queue.
        @rtype: C{int}
        """
        return self.size(destination) > 0

    @synchronized(lock)
    def destinations(self):
        """
        Provides a set of destinations (queue "addresses") available.

        @return: A list of the detinations available.
        @rtype: C{set}
        """
        raise NotImplementedError

    @synchronized(lock)
    def close(self):
        """
        May be implemented to perform any necessary cleanup operations when store is closed.
        """
        pass

    # This is intentionally not synchronized, since it does not directly
    # expose any shared data.
    def frames(self, destination):
        """
        Returns an iterator for frames in specified queue.

        The iterator simply wraps calls to L{dequeue} method, so the order of the 
        frames from the iterator will be the reverse of the order in which the
        frames were enqueued.

        @param destination: The queue destination (e.g. /queue/foo)
        @type destination: C{str}
        """
        return QueueFrameIterator(self, destination)


class QueueFrameIterator(object):
    """
    Provides an C{iterable} over the frames for a specified destination in a queue.

    @ivar store: The queue store.
    @type store: L{coilmq.store.QueueStore}

    @ivar destination: The destination for this iterator.
    @type destination: C{str}
    """

    def __init__(self, store, destination):
        self.store = store
        self.destination = destination

    def __iter__(self):
        return self

    def next(self):
        return self.__next__()

    def __next__(self):
        frame = self.store.dequeue(self.destination)
        if not frame:
            raise StopIteration()
        return frame

    def __len__(self):
        return self.store.size(self.destination)


class TopicStore(object):
    """
    Abstract base class for non-durable topic storage.
    """


class DurableTopicStore(TopicStore):
    """
    Abstract base class for durable topic storage.
    """
