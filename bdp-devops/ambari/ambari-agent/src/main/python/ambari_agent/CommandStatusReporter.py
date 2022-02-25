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

import logging
import threading

logger = logging.getLogger(__name__)

class CommandStatusReporter(threading.Thread):
  def __init__(self, initializer_module):
    self.initializer_module = initializer_module
    self.commandStatuses = initializer_module.commandStatuses
    self.stop_event = initializer_module.stop_event
    self.command_reports_interval = initializer_module.config.command_reports_interval
    threading.Thread.__init__(self)

  def run(self):
    """
    Run an endless loop which reports all the commands results (IN_PROGRESS, FAILED, COMPLETE) every self.command_reports_interval seconds.
    """
    if self.command_reports_interval == 0:
      logger.warn("CommandStatusReporter is turned off. Some functionality might not work correctly.")
      return

    while not self.stop_event.is_set():
      try:
        if self.initializer_module.is_registered:
          self.commandStatuses.report()
      except:
        logger.exception("Exception in CommandStatusReporter. Re-running it")

      self.stop_event.wait(self.command_reports_interval)

    logger.info("CommandStatusReporter has successfully finished")
