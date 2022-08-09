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


from collections import deque
from threading import Event


class BufferedQueue(object):
  """
  Thread safe buffered queue
  """

  def __init__(self):
    self.__queue = deque()
    self.__data_ready_event = Event()

    self.__queue_end = False  # sign that buffer is empty
    self.__queue_feeder_end = False    # EOF sign

  def __notify_ready(self):
    """
    Notify reader that data is ready to be consumed
    """
    self.__queue_end = False
    self.__data_ready_event.set()

  def notify_end(self):
    """
    Notify queue about end of producer stream, allow consumer to read buffer to the end
    """
    self.__queue_feeder_end = True
    self.__notify_ready()

  def put(self, item):
    """
    Add object to the buffer
    """
    if self.__queue_feeder_end:
      raise IndexError("'notify_end' was called, queue is locked for writing")

    self.__queue.append(item)
    self.__notify_ready()

  def get(self, timeout=None):
    """
    Read data from buffer at least in `timeout` seconds. If no data ready in `timeout`, would be returned None.

    :param timeout: amount of time to wait for data availability
    :return: data or None if no data were read in `timeout` or no more data available (buffer is empty)
    """
    try:
      if not self.__queue_feeder_end:
        self.__data_ready_event.wait(timeout)
      return self.__queue.popleft()
    except IndexError:
      if timeout:
        return None

      self.__queue_end = True
    finally:
      if self.count == 0:
        self.__data_ready_event.clear()
        if self.__queue_feeder_end:
          self.__queue_end = True

  def reset(self):
    """
    Clear instance state and data
    """
    self.__data_ready_event.clear()
    self.__queue.clear()
    self.__queue_feeder_end = False
    self.__queue_end = False

  @property
  def empty(self):
    if self.__queue_feeder_end and self.count == 0:
      return True

    return self.__queue_end

  @property
  def count(self):
    return len(self.__queue)
