try:
    import redis
except ImportError:  # pragma: no cover
    import sys; sys.exit('please, install redis-py package to use redis-store')
import threading
try:
    import cPickle as pickle
except ImportError:
    import pickle

from coilmq.store import QueueStore
from coilmq.util.concurrency import synchronized
from coilmq.config import config

__authors__ = ('"Hans Lellelid" <hans@xmpl.org>', '"Alexander Zhukov" <zhukovaa90@gmail.com>')
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


def make_redis_store(cfg=None):
    return RedisQueueStore(
        redis_conn=redis.Redis(**dict((cfg or config).items('redis'))))


class RedisQueueStore(QueueStore):
    """Simple Queue with Redis Backend"""
    def __init__(self, redis_conn=None):
        """The default connection parameters are: host='localhost', port=6379, db=0"""
        self.__db = redis_conn or redis.Redis()
        # self.key = '{0}:{1}'.format(namespace, name)
        super(RedisQueueStore, self).__init__()

    @synchronized(lock)
    def enqueue(self, destination, frame):
        self.__db.rpush(destination, pickle.dumps(frame))

    @synchronized(lock)
    def dequeue(self, destination):
        item = self.__db.lpop(destination)
        if item:
            return pickle.loads(item)

    @synchronized(lock)
    def requeue(self, destination, frame):
        self.enqueue(destination, frame)

    @synchronized(lock)
    def size(self, destination):
        return self.__db.llen(destination)

    @synchronized(lock)
    def has_frames(self, destination):
        return self.size(destination) > 0

    @synchronized(lock)
    def destinations(self):
        return self.__db.keys()
