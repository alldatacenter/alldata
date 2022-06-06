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

import os
import logging
import atexit

logger = logging.getLogger(__name__)

__all__ = ["ExitHelper"]

class _singleton(type):
  _instances = {}

  def __call__(cls, *args, **kwargs):
    if cls not in cls._instances:
      cls._instances[cls] = super(_singleton, cls).__call__(*args, **kwargs)
    return cls._instances[cls]


class ExitHelper(object):
  """
  Class to cleanup resources before exiting. Replacement for atexit module. sys.exit(code) works only from threads and
  os._exit(code) will ignore atexit and cleanup will be ignored.

  WARNING: always import as `ambari_agent.ExitHelper import ExitHelper`, otherwise it will be imported twice and nothing
  will work as expected.
  """
  __metaclass__ = _singleton

  def __init__(self):
    self.exit_functions = []
    self.exit_functions_executed = False
    self.exitcode = 0
    atexit.register(self.execute_cleanup)

  def execute_cleanup(self):
    if self.exit_functions_executed:
      return
    logger.info("Performing cleanup before exiting...")
    while self.exit_functions:
      func, args, kwargs = self.exit_functions.pop()
      try:
        func(*args, **kwargs)
      except:
        pass
    self.exit_functions_executed = True

  def register(self, func, *args, **kwargs):
    self.exit_functions.append((func, args, kwargs))

  def exit(self):
    self.execute_cleanup()
    logger.info("Cleanup finished, exiting with code:" + str(self.exitcode))
    os._exit(self.exitcode)


if __name__ == '__main__':
  def func1():
    print "1"

  def func2():
    print "2"

  ExitHelper().register(func1)
  ExitHelper().register(func2)
  ExitHelper().exit(3)