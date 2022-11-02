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

from ambari_commons.exceptions import FatalException
from ambari_commons.os_check import OSConst, OSCheck
import os
import logging
import signal
import threading
import traceback
from ambari_commons.os_family_impl import OsFamilyImpl

from ambari_agent.RemoteDebugUtils import bind_debug_signal_handlers

logger = logging.getLogger()

_handler = None

class HeartbeatStopHandlers(object):pass

# windows impl

@OsFamilyImpl(os_family=OSConst.WINSRV_FAMILY)
class HeartbeatStopHandlersWindows(HeartbeatStopHandlers):
  def __init__(self, stopEvent=None):
    import win32event
    # Event is used for synchronizing heartbeat iterations (to make possible
    # manual wait() interruption between heartbeats )
    self._heventHeartbeat = win32event.CreateEvent(None, 0, 0, None)

    # Event is used to stop the Agent process
    if stopEvent is None:
      # Allow standalone testing
      self._heventStop = win32event.CreateEvent(None, 0, 0, None)
    else:
      # Allow one unique event per process
      self._heventStop = stopEvent

  def set_heartbeat(self):
    import win32event

    win32event.SetEvent(self._heventHeartbeat)

  def reset_heartbeat(self):
    import win32event

    win32event.ResetEvent(self._heventHeartbeat)

  def wait(self, timeout1, timeout2=0):
    import win32event

    timeout = int(timeout1 + timeout2) * 1000

    result = win32event.WaitForMultipleObjects([self._heventStop, self._heventHeartbeat], False, timeout)
    if (
          win32event.WAIT_OBJECT_0 != result and win32event.WAIT_OBJECT_0 + 1 != result and win32event.WAIT_TIMEOUT != result):
      raise FatalException(-1, "Error waiting for stop/heartbeat events: " + str(result))
    if (win32event.WAIT_TIMEOUT == result):
      return -1
    return result # 0 -> stop, 1 -> heartbeat

# linux impl

def signal_handler(signum, frame):
  logger.info("Ambari-agent received {0} signal, stopping...".format(signum))
  _handler.set()


def debug(sig, frame):
  """Interrupt running process, and provide a stacktrace of threads """
  d = {'_frame': frame}  # Allow access to frame object.
  d.update(frame.f_globals)  # Uamnless shadowed by global
  d.update(frame.f_locals)

  message = "Signal received.\nTraceback:\n"
  message += ''.join(traceback.format_stack(frame))
  logger.info(message)


@OsFamilyImpl(os_family=OsFamilyImpl.DEFAULT)
class HeartbeatStopHandlersLinux(HeartbeatStopHandlers):
  def __init__(self):
    self.heartbeat_wait_event = threading.Event()
    self._stop = False

  def set_heartbeat(self):
    self.heartbeat_wait_event.set()

  def reset_heartbeat(self):
    self.heartbeat_wait_event.clear()

  def set_stop(self):
    self._stop = True

  def wait(self, timeout1, timeout2=0):
    if self._stop:
      logger.info("Stop event received")
      return 0

    if self.heartbeat_wait_event.wait(timeout=timeout1):
      return 1
    return -1




def bind_signal_handlers(agentPid, stop_event):
  global _handler
  if OSCheck.get_os_family() != OSConst.WINSRV_FAMILY:
    if os.getpid() == agentPid:
      signal.signal(signal.SIGINT, signal_handler)
      signal.signal(signal.SIGTERM, signal_handler)

      bind_debug_signal_handlers()

    _handler = stop_event
  else:
    _handler = stop_event
  return _handler
