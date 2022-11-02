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

Ambari Agent

"""

from resource_management.core.logger import Logger

class FcntlBasedProcessLock(object):
  """A file descriptor based lock for interprocess locking.
  The lock is automatically released when process dies.

  WARNING: A file system and OS must support fcntl.lockf.
  Doesn't work on Windows systems. Doesn't work properly on
  some NFS implementations.

  Currently Ambari uses FcntlBasedProcessLock only when parallel
  execution is enabled on the agent.

  WARNING: Do not use this lock for synchronization between threads.
  Multiple threads in a same process can simultaneously acquire this lock.
  It should be used only for locking between processes.
  """

  def __init__(self, lock_file_path, skip_fcntl_failures, enabled = True):
    """
    :param lock_file_path: The path to the file used for locking
    :param skip_fcntl_failures: Use this only if the lock is not mandatory.
                                If set to True, the lock will ignore fcntl call failures.
                                Locking will not work, if fcntl is not supported.
                                skip_fcntl_failures prevents exceptions raising in this case.
    :param enabled: If is set to False, fcntl will not be imported and lock/unlock methods return immediately.
    """
    self.lock_file_name = lock_file_path
    self.lock_file = None
    self.acquired = False
    self.skip_fcntl_failures = skip_fcntl_failures
    self.enabled = enabled

  def blocking_lock(self):
    """
    Creates the lock file if it doesn't exist.
    Waits to acquire an exclusive lock on the lock file descriptor.
    """
    if not self.enabled:
      return
    import fcntl
    Logger.info("Trying to acquire a lock on {0}".format(self.lock_file_name))
    if self.lock_file is None or self.lock_file.closed:
      self.lock_file = open(self.lock_file_name, 'a')
    try:
      fcntl.lockf(self.lock_file, fcntl.LOCK_EX)
    except:
      if self.skip_fcntl_failures:
        Logger.exception("Fcntl call raised an exception. A lock was not aquired. "
                         "Continuing as skip_fcntl_failures is set to True")
      else:
        raise
    else:
      self.acquired = True
      Logger.info("Acquired the lock on {0}".format(self.lock_file_name))

  def unlock(self):
    """
    Unlocks the lock file descriptor.
    """
    if not self.enabled:
      return
    import fcntl
    Logger.info("Releasing the lock on {0}".format(self.lock_file_name))
    if self.acquired:
      try:
        fcntl.lockf(self.lock_file, fcntl.LOCK_UN)
      except:
        if self.skip_fcntl_failures:
          Logger.exception("Fcntl call raised an exception. The lock was not released. "
                           "Continuing as skip_fcntl_failures is set to True")
        else:
          raise
      else:
        self.acquired = False
        try:
          self.lock_file.close()
          self.lock_file = None
        except IOError:
          Logger.warning("Failed to close {0}".format(self.lock_file_name))

  def __enter__(self):
    self.blocking_lock()
    return None

  def __exit__(self, exc_type, exc_val, exc_tb):
    self.unlock()
    return False

