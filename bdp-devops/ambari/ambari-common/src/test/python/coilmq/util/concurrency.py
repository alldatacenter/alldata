"""
Tools to facilitate developing thread-safe components.
"""

import abc
import threading
import functools

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


def synchronized(lock):
    def synchronize(func):
        """
        Decorator to lock and unlock a method (Phillip J. Eby).

        This function is to be used with object instance methods; the object must
        have a _lock variable (of type C{threading.Lock} or C{threading.RLock}).

        @param func: Method to decorate
        @type func: C{callable}
        """
        @functools.wraps(func)
        def wrapper(*args, **kwargs):
            with lock:
                return func(*args, **kwargs)
        return wrapper
    return synchronize


class CoilTimerBase(object):

    __metaclass__ = abc.ABCMeta

    def __init__(self):
        self.jobs = []

    def schedule(self, period, callback):
        self.jobs.append((period, callback))

    @abc.abstractmethod
    def run(self):
        raise NotImplementedError

    @abc.abstractmethod
    def start(self):
        raise NotImplementedError

    @abc.abstractmethod
    def stop(self):
        raise NotImplementedError

    def __enter__(self):
        self.start()

    def __exit__(self, exc_type, exc_val, exc_tb):
        self.stop()


# TODO: check against following notes
# <http://stackoverflow.com/questions/2124540/how-does-timer-in-python-work-regarding-mutlithreading>
# <http://stackoverflow.com/questions/12435211/python-threading-timer-repeat-function-every-n-seconds>
class CoilThreadingTimer(CoilTimerBase):

    def __init__(self, *args, **kwargs):
        super(CoilThreadingTimer, self).__init__(*args, **kwargs)
        self._running = False

    def run(self):
        def run_job(interval, callback):
            if self._running:
                threading.Timer(interval, run_job, args=(interval, callback)).start()
                callback()
        for period, job in self.jobs:
            run_job(period, job)

    def start(self):
        self._running = True
        self.run()

    def stop(self):
        self._running = False


